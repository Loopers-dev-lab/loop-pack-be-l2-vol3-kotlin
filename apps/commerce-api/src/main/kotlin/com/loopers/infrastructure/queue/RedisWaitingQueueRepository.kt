package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RedisWaitingQueueRepository(
    @param:Qualifier(REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : WaitingQueueRepository {

    companion object {
        private const val QUEUE_KEY = "waiting-queue"
        private const val TOKEN_KEY_PREFIX = "entry-token:"

        /**
         * Lua 스크립트: 원자적으로 상한 검증 + 대기열 진입 수행.
         *
         * KEYS[1] = waiting-queue
         * ARGV[1] = score (timestamp)
         * ARGV[2] = userId (member)
         * ARGV[3] = maxCapacity
         *
         * 반환: 순번 (0-based) 또는 -1 (상한 초과)
         */
        private val ENTER_SCRIPT = RedisScript.of(
            """
            local existingRank = redis.call('ZRANK', KEYS[1], ARGV[2])
            if existingRank then
                return existingRank
            end
            local currentCount = redis.call('ZCARD', KEYS[1])
            if currentCount >= tonumber(ARGV[3]) then
                return -1
            end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
            return redis.call('ZRANK', KEYS[1], ARGV[2])
            """.trimIndent(),
            Long::class.java,
        )

        /**
         * Lua 스크립트: 원자적으로 N명을 pop하고 각각 입장 토큰을 SET.
         * "절대로 먼저 삭제하지 마라" 원칙: SET entry-token 후 ZREM 수행.
         *
         * KEYS[1] = waiting-queue
         * ARGV[1] = count
         * ARGV[2] = ttlSeconds
         * ARGV[3] = tokenKeyPrefix (예: "entry-token:")
         * ARGV[4..] = token[i] (Kotlin에서 미리 생성한 UUID)
         *
         * 반환: pop된 member(userId) 리스트
         */
        private val POP_AND_ISSUE_SCRIPT = RedisScript.of(
            """
            local count = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local prefix = ARGV[3]
            local members = redis.call('ZRANGE', KEYS[1], 0, count - 1)
            local result = {}
            for i, member in ipairs(members) do
                local token = ARGV[3 + i]
                redis.call('SET', prefix .. member, token, 'EX', ttl)
                redis.call('ZREM', KEYS[1], member)
                table.insert(result, member)
            end
            return result
            """.trimIndent(),
            List::class.java,
        )
    }

    override fun enter(userId: UserId, score: Double, maxCapacity: Int): Long? {
        val result = redisTemplate.execute(
            ENTER_SCRIPT,
            listOf(QUEUE_KEY),
            score.toString(),
            userId.value.toString(),
            maxCapacity.toString(),
        )
        return if (result == -1L) null else result
    }

    override fun findPosition(userId: UserId): Long? {
        return redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.value.toString())
    }

    override fun count(): Long {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY) ?: 0L
    }

    override fun popMin(count: Int): List<UserId> {
        val tuples = redisTemplate.opsForZSet().popMin(QUEUE_KEY, count.toLong())
            ?: return emptyList()
        return tuples.map { tuple ->
            val value = tuple.value
                ?: throw IllegalStateException("대기열에 비정상 member가 존재합니다: null")
            val id = value.toLongOrNull()
                ?: throw IllegalStateException("대기열에 비정상 member가 존재합니다: $value")
            UserId(id)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun popMinAndIssueTokens(count: Int, ttlSeconds: Long): List<Pair<UserId, String>> {
        val tokens = List(count) { UUID.randomUUID().toString() }
        val argv = buildList {
            add(count.toString())
            add(ttlSeconds.toString())
            add(TOKEN_KEY_PREFIX)
            addAll(tokens)
        }
        val members = redisTemplate.execute(
            POP_AND_ISSUE_SCRIPT,
            listOf(QUEUE_KEY),
            *argv.toTypedArray(),
        ) as? List<String> ?: return emptyList()

        return members.mapIndexed { index, member ->
            val id = member.toLongOrNull()
                ?: throw IllegalStateException("대기열에 비정상 member가 존재합니다: $member")
            UserId(id) to tokens[index]
        }
    }
}
