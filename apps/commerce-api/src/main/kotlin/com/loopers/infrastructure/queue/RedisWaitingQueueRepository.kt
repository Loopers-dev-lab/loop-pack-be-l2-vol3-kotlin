package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueuePosition
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class RedisWaitingQueueRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : WaitingQueueRepository {
    override fun enter(userId: Long): QueuePosition {
        val results = redisTemplate.execute(
            ENTER_SCRIPT,
            listOf(KEY, SEQ_KEY),
            userId.toString(),
        )
        requireNotNull(results) { "Redis ENTER_SCRIPT returned null for userId=$userId" }
        require(results.size == 2) { "Redis ENTER_SCRIPT returned unexpected result size: ${results.size}" }
        return QueuePosition(
            position = results[0] as Long,
            totalWaiting = results[1] as Long,
        )
    }

    override fun getPosition(userId: Long): QueuePosition? {
        val ops = redisTemplate.opsForZSet()
        val rank = ops.rank(KEY, userId.toString()) ?: return null
        val total = ops.size(KEY) ?: 0L
        return QueuePosition(position = rank, totalWaiting = total)
    }

    override fun size(): Long = redisTemplate.opsForZSet().size(KEY) ?: 0L

    override fun remove(userId: Long) {
        redisTemplate.opsForZSet().remove(KEY, userId.toString())
    }

    override fun popFront(count: Long): List<Long> {
        val results = redisTemplate.opsForZSet().popMin(KEY, count)
        return results?.mapNotNull { it.value?.toLongOrNull() } ?: emptyList()
    }

    companion object {
        private const val KEY = "queue:waiting"
        private const val SEQ_KEY = "queue:waiting:seq"

        private val ENTER_SCRIPT = RedisScript.of<List<*>>(
            """
            local seq = redis.call('INCR', KEYS[2])
            redis.call('ZADD', KEYS[1], 'NX', seq, ARGV[1])
            local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
            local total = redis.call('ZCARD', KEYS[1])
            return {rank, total}
            """.trimIndent(),
            List::class.java,
        )
    }
}
