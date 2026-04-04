package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueTokenStore
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

@Component
class RedisQueueTokenStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
) : QueueTokenStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TOKEN_KEY_PREFIX = "queue:entry-token:"
        private const val TOKEN_MEMBERS_KEY = "queue:entry-token:members"

        // Lua: SET NX EX token key + SADD member to tracking set (atomic)
        private val ISSUE_SCRIPT = DefaultRedisScript<Long>(
            """
            local key = KEYS[1]
            local set = KEYS[2]
            local token = ARGV[1]
            local ttl = tonumber(ARGV[2])
            local memberId = ARGV[3]
            if redis.call('SET', key, token, 'NX', 'EX', ttl) then
                redis.call('SADD', set, memberId)
                return 1
            end
            return 0
            """.trimIndent(),
            Long::class.java,
        )

        // Lua: DEL token key + SREM member from tracking set (atomic)
        private val DELETE_SCRIPT = DefaultRedisScript<Long>(
            """
            local key = KEYS[1]
            local set = KEYS[2]
            local memberId = ARGV[1]
            local deleted = redis.call('DEL', key)
            if deleted == 1 then
                redis.call('SREM', set, memberId)
            end
            return deleted
            """.trimIndent(),
            Long::class.java,
        )
    }

    override fun issue(memberId: Long, token: String, ttlSeconds: Long): Boolean {
        return try {
            val result = masterRedisTemplate.execute(
                ISSUE_SCRIPT,
                listOf(tokenKey(memberId), TOKEN_MEMBERS_KEY),
                token,
                ttlSeconds.toString(),
                memberId.toString(),
            )
            result == 1L
        } catch (e: Exception) {
            log.warn("[QueueTokenStore] 토큰 발급 실패 (memberId={})", memberId, e)
            false
        }
    }

    override fun get(memberId: Long): String? {
        return try {
            redisTemplate.opsForValue().get(tokenKey(memberId))
        } catch (e: Exception) {
            log.warn("[QueueTokenStore] 토큰 조회 실패 (memberId={})", memberId, e)
            null
        }
    }

    override fun delete(memberId: Long): Boolean {
        return try {
            val result = masterRedisTemplate.execute(
                DELETE_SCRIPT,
                listOf(tokenKey(memberId), TOKEN_MEMBERS_KEY),
                memberId.toString(),
            )
            result == 1L
        } catch (e: Exception) {
            log.warn("[QueueTokenStore] 토큰 삭제 실패 (memberId={})", memberId, e)
            false
        }
    }

    override fun activeCount(): Long {
        return try {
            val members = redisTemplate.opsForSet().members(TOKEN_MEMBERS_KEY) ?: return 0L
            if (members.isEmpty()) return 0L

            // Pipeline으로 일괄 조회 (N번 round-trip → 1번)
            val tokenResults = redisTemplate.executePipelined { connection ->
                members.forEach { memberId ->
                    connection.stringCommands().get(tokenKey(memberId.toLong()).toByteArray())
                }
                null
            }

            val expired = members.zip(tokenResults)
                .filter { (_, token) -> token == null }
                .map { (memberId, _) -> memberId }

            if (expired.isNotEmpty()) {
                masterRedisTemplate.opsForSet().remove(TOKEN_MEMBERS_KEY, *expired.toTypedArray())
            }
            (members.size - expired.size).toLong().coerceAtLeast(0)
        } catch (e: Exception) {
            log.warn("[QueueTokenStore] 활성 토큰 수 조회 실패", e)
            0L
        }
    }

    private fun tokenKey(memberId: Long) = "$TOKEN_KEY_PREFIX$memberId"
}
