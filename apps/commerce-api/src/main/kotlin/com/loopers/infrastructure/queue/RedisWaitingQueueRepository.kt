package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
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
    }

    override fun enter(userId: Long, score: Double, maxCapacity: Int): Long? {
        val result = redisTemplate.execute(
            ENTER_SCRIPT,
            listOf(QUEUE_KEY),
            score.toString(),
            userId.toString(),
            maxCapacity.toString(),
        )
        return if (result == -1L) null else result
    }

    override fun findPosition(userId: Long): Long? {
        return redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString())
    }

    override fun count(): Long {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY) ?: 0L
    }

    override fun popMin(count: Int): List<Long> {
        val tuples = redisTemplate.opsForZSet().popMin(QUEUE_KEY, count.toLong())
            ?: return emptyList()
        return tuples.mapNotNull { it.value?.toLongOrNull() }
    }
}
