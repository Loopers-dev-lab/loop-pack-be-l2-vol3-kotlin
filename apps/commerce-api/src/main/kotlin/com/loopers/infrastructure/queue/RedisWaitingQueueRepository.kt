package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

@Repository
class RedisWaitingQueueRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : WaitingQueueRepository {

    companion object {
        private const val QUEUE_KEY = "waiting-queue"

        /**
         * Lua 스크립트: 원자적으로 상한 검증 + 대기열 진입 수행.
         * score는 Redis 서버 타임스탬프(TIME 커맨드)로 생성하여 Double 정밀도 한계를 회피한다.
         * score = seconds * 1_000_000 + microseconds (마이크로초 단위 유닉스 타임스탬프)
         * 최대값 ≈ 1.7×10^15 < 2^53 (Double 안전 정수 범위) 이므로 정밀도 손실 없음.
         *
         * KEYS[1] = waiting-queue
         * ARGV[1] = userId (member)
         * ARGV[2] = maxCapacity
         *
         * 반환: 순번 (0-based) 또는 -1 (상한 초과)
         */
        private val ENTER_SCRIPT = RedisScript.of(
            """
            local existingRank = redis.call('ZRANK', KEYS[1], ARGV[1])
            if existingRank then
                return existingRank
            end
            local currentCount = redis.call('ZCARD', KEYS[1])
            if currentCount >= tonumber(ARGV[2]) then
                return -1
            end
            local time = redis.call('TIME')
            local score = tonumber(time[1]) * 1000000 + tonumber(time[2])
            redis.call('ZADD', KEYS[1], score, ARGV[1])
            return redis.call('ZRANK', KEYS[1], ARGV[1])
            """.trimIndent(),
            Long::class.java,
        )
    }

    override fun enter(userId: UserId, maxCapacity: Int): Long? {
        val result = redisTemplate.execute(
            ENTER_SCRIPT,
            listOf(QUEUE_KEY),
            userId.value.toString(),
            maxCapacity.toString(),
        )
        return when {
            result == -1L -> null
            result != null && result >= 0 -> result
            else -> throw CoreException(ErrorType.INTERNAL_ERROR, "대기열 진입 Lua 스크립트 예상 밖 반환값: $result")
        }
    }

    override fun findPosition(userId: UserId): Long? {
        return redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.value.toString())
    }

    override fun count(): Long {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY) ?: 0L
    }

    override fun popMin(count: Int): List<UserId> {
        if (count <= 0) return emptyList()
        val tuples = redisTemplate.opsForZSet().popMin(QUEUE_KEY, count.toLong())
            ?: return emptyList()
        return tuples.map { tuple ->
            val value = tuple.value
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열에 비정상 member가 존재합니다: null")
            val id = value.toLongOrNull()
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열에 비정상 member가 존재합니다: $value")
            UserId(id)
        }
    }
}
