package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class WaitingQueueRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val redisTemplate: RedisTemplate<String, String>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun addToQueue(userId: Long): Long {
        val score = System.currentTimeMillis().toDouble()
        masterRedisTemplate.opsForZSet().add(QUEUE_KEY, userId.toString(), score)
        // ZADD 직후 master에서 ZRANK 조회 (replica lag 방지)
        val rank = masterRedisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString())
        return (rank ?: 0) + 1 // 0-based → 1-based
    }

    fun getPosition(userId: Long): Long? {
        val rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId.toString())
        return rank?.let { it + 1 } // 0-based → 1-based
    }

    fun getTotalCount(): Long {
        return redisTemplate.opsForZSet().zCard(QUEUE_KEY) ?: 0
    }

    /**
     * Lua 스크립트로 ZPOPMIN + SET EX를 원자적으로 실행한다.
     * 대기열에서 N명을 꺼내고 각각 토큰을 발급한다.
     * 스케줄러가 ZPOPMIN 후 죽어도 유저가 유실되지 않도록 원자성을 보장한다.
     *
     * @return 토큰이 발급된 userId 목록
     */
    fun popAndIssueTokens(count: Long): List<Long> {
        val result = masterRedisTemplate.execute(
            POP_AND_ISSUE_SCRIPT,
            listOf(QUEUE_KEY),
            count.toString(),
            TOKEN_KEY_PREFIX,
            TOKEN_TTL.seconds.toString(),
        )

        return result?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    fun getToken(userId: Long): String? {
        return redisTemplate.opsForValue().get("$TOKEN_KEY_PREFIX$userId")
    }

    fun deleteToken(userId: Long) {
        masterRedisTemplate.delete("$TOKEN_KEY_PREFIX$userId")
    }

    fun hasToken(userId: Long): Boolean {
        return redisTemplate.hasKey("$TOKEN_KEY_PREFIX$userId")
    }

    fun isInQueue(userId: Long): Boolean {
        return redisTemplate.opsForZSet().score(QUEUE_KEY, userId.toString()) != null
    }

    companion object {
        const val QUEUE_KEY = "queue:waiting:order"
        const val TOKEN_KEY_PREFIX = "queue:token:"
        val TOKEN_TTL: Duration = Duration.ofMinutes(5)

        /**
         * Lua 스크립트: ZPOPMIN으로 N명 꺼내고, 각각 UUID 토큰을 SET EX로 발급
         * KEYS[1] = queue:waiting:order
         * ARGV[1] = count (꺼낼 인원 수)
         * ARGV[2] = token key prefix (queue:token:)
         * ARGV[3] = TTL (초)
         *
         * 반환: 토큰이 발급된 userId 목록
         */
        private val POP_AND_ISSUE_SCRIPT = RedisScript.of<List<String>>(
            """
            local results = redis.call('ZPOPMIN', KEYS[1], ARGV[1])
            local issued = {}
            for i = 1, #results, 2 do
                local userId = results[i]
                local token = redis.call('TIME')
                local tokenValue = userId .. ':' .. token[1] .. token[2]
                redis.call('SET', ARGV[2] .. userId, tokenValue, 'EX', ARGV[3])
                table.insert(issued, userId)
            end
            return issued
            """.trimIndent(),
        )
    }
}
