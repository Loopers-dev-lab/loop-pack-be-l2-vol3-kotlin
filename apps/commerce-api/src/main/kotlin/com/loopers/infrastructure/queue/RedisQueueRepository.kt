package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueueEntry
import com.loopers.domain.queue.QueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

/**
 * Redis Sorted Set 기반 대기열 구현체.
 *
 * Score 설계:
 * - Redis TIME 명령으로 마이크로초 정밀도를 확보한다.
 * - 모든 서버가 Redis 서버의 시계를 기준으로 하므로 서버 간 시계 차이 문제가 없다.
 * - ZADD + ZRANK를 Lua 스크립트로 원자적으로 실행한다.
 */
@Repository
class RedisQueueRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : QueueRepository {

    /**
     * 대기열에 유저를 추가한다.
     * Lua 스크립트로 Redis TIME(마이크로초)을 score로 사용 + NX 옵션.
     * @return true면 신규 추가, false면 이미 존재
     */
    override fun add(userId: Long): Boolean {
        val result = redisTemplate.execute(
            ADD_SCRIPT,
            listOf(QueueRedisKeys.WAITING_QUEUE),
            userId.toString(),
        )
        return result == 1L
    }

    override fun getRank(userId: Long): Long? {
        return redisTemplate.opsForZSet()
            .rank(QueueRedisKeys.WAITING_QUEUE, userId.toString())
    }

    override fun size(): Long {
        return redisTemplate.opsForZSet()
            .size(QueueRedisKeys.WAITING_QUEUE) ?: 0L
    }

    override fun popMin(count: Long): List<QueueEntry> {
        if (count <= 0) return emptyList()

        val result = redisTemplate.opsForZSet()
            .popMin(QueueRedisKeys.WAITING_QUEUE, count)
            ?: return emptyList()

        return result.mapNotNull { typedTuple ->
            val value = typedTuple.value ?: return@mapNotNull null
            val score = typedTuple.score ?: return@mapNotNull null
            QueueEntry(userId = value.toLong(), score = score)
        }
    }

    override fun isEnabled(): Boolean {
        return redisTemplate.opsForValue().get(QueueRedisKeys.ENABLED) == "true"
    }

    override fun enable() {
        redisTemplate.opsForValue().set(QueueRedisKeys.ENABLED, "true")
    }

    override fun disable() {
        redisTemplate.opsForValue().set(QueueRedisKeys.ENABLED, "false")
    }

    /**
     * waiting → processing 원자적 이동 (Lua).
     * score = Redis TIME 기반 밀리초 (claimed_at).
     * 서버 죽어도 processing에 남아있어 복구 가능.
     */
    @Suppress("UNCHECKED_CAST")
    override fun moveToProcessing(count: Long): List<QueueEntry> {
        if (count <= 0) return emptyList()
        val result = redisTemplate.execute(
            MOVE_TO_PROCESSING_SCRIPT,
            listOf(QueueRedisKeys.WAITING_QUEUE, QueueRedisKeys.PROCESSING_QUEUE),
            count.toString(),
        ) as? List<*> ?: return emptyList()

        return result.chunked(2).mapNotNull { chunk ->
            if (chunk.size < 2) return@mapNotNull null
            val userId = (chunk[0] as? String)?.toLongOrNull() ?: return@mapNotNull null
            val score = (chunk[1] as? String)?.toDoubleOrNull() ?: return@mapNotNull null
            QueueEntry(userId = userId, score = score)
        }
    }

    override fun removeFromProcessing(userIds: List<Long>) {
        if (userIds.isEmpty()) return
        redisTemplate.opsForZSet().remove(
            QueueRedisKeys.PROCESSING_QUEUE,
            *userIds.map { it.toString() }.toTypedArray(),
        )
    }

    /**
     * 처리 중 만료 항목을 waiting으로 복구 (Lua 원자적).
     * ZRANGEBYSCORE로 만료된 항목 조회 → ZREM processing + ZADD waiting NX.
     * ZADD NX: 유저가 이미 재진입했으면 순번을 덮어쓰지 않음.
     */
    override fun recoverExpiredToWaiting(beforeTimestampMs: Long): Int {
        val result = redisTemplate.execute(
            RECOVER_EXPIRED_SCRIPT,
            listOf(QueueRedisKeys.PROCESSING_QUEUE, QueueRedisKeys.WAITING_QUEUE),
            beforeTimestampMs.toString(),
        )
        return (result as? Long)?.toInt() ?: 0
    }

    companion object {
        /**
         * ZADD NX + Redis TIME 기반 score 생성.
         * Redis TIME은 [초, 마이크로초]를 반환.
         * score = 초 * 1_000_000 + 마이크로초 → 마이크로초 정밀도.
         */
        private val ADD_SCRIPT = RedisScript.of<Long>(
            """
            local time = redis.call('TIME')
            local score = tonumber(time[1]) * 1000000 + tonumber(time[2])
            return redis.call('ZADD', KEYS[1], 'NX', score, ARGV[1])
            """.trimIndent(),
            Long::class.java,
        )

        /**
         * waiting → processing 원자적 이동.
         * KEYS[1]=waiting, KEYS[2]=processing, ARGV[1]=count
         * score = Redis TIME 기반 밀리초 (claimed_at, 만료 판단 기준).
         * 반환: [userId, originalScore, userId, originalScore, ...]
         */
        @Suppress("UNCHECKED_CAST")
        private val MOVE_TO_PROCESSING_SCRIPT = RedisScript.of(
            """
            local entries = redis.call('ZPOPMIN', KEYS[1], ARGV[1])
            if #entries == 0 then return {} end
            local time = redis.call('TIME')
            local claimed_at = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
            for i=1,#entries,2 do
                redis.call('ZADD', KEYS[2], claimed_at, entries[i])
            end
            return entries
            """.trimIndent(),
            List::class.java,
        ) as RedisScript<List<*>>

        /**
         * 처리 중 만료 항목을 waiting으로 복구 (Lua 원자적).
         *
         * [왜 Lua 스크립트로 묶어야 하는가]
         * 이 작업을 3번의 개별 Redis 호출로 쪼개면:
         *   1) ZRANGEBYSCORE → 만료 목록 조회
         *   2) ZREM          → processing에서 제거
         *   3) ZADD NX       → waiting에 복구
         * 1)과 2) 사이에 다른 스케줄러 인스턴스가 동일 항목을 ZREM하면 중복 복구 발생.
         * Lua 스크립트는 Redis에서 원자적으로 실행되므로 이 gap이 없다.
         *
         * [각 명령의 역할]
         * KEYS[1] = {queue}:processing  (만료 판단 대상)
         * KEYS[2] = {queue}:waiting     (복구 목적지)
         * ARGV[1] = cutoff_millis       (이 시각 이전에 claimed된 항목 = 만료 간주)
         *
         * ZRANGEBYSCORE KEYS[1] -inf ARGV[1]
         *   → processing에서 claimed_at ≤ cutoff인 항목(userId 목록) 조회
         *     (processQueue가 moveToProcessing 후 서버가 죽으면 이 상태로 남음)
         *
         * ZREM KEYS[1] expired[i]
         *   → 해당 항목을 processing에서 제거
         *
         * ZADD KEYS[2] NX base_score+i expired[i]
         *   → waiting에 복구. NX 옵션으로 이미 재진입한 유저의 순번은 덮어쓰지 않음
         *     (서버가 죽은 사이 유저가 직접 재진입했을 경우를 보호)
         *
         * base_score = Redis TIME 마이크로초 + i 오프셋
         *   → 복구 항목끼리도 상대적 순서를 유지 (같은 마이크로초에 여러 명 복구 시 충돌 방지)
         *   → waiting은 score 오름차순이므로 복구 유저는 현재 대기자 뒤에 붙음
         *
         * 반환: 복구된 인원 수
         */
        private val RECOVER_EXPIRED_SCRIPT = RedisScript.of<Long>(
            """
            local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            if #expired == 0 then return 0 end
            local time = redis.call('TIME')
            local base_score = tonumber(time[1]) * 1000000 + tonumber(time[2])
            for i=1,#expired do
                redis.call('ZREM', KEYS[1], expired[i])
                redis.call('ZADD', KEYS[2], 'NX', base_score + i, expired[i])
            end
            return #expired
            """.trimIndent(),
            Long::class.java,
        )
    }
}
