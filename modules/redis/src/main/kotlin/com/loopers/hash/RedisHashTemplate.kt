package com.loopers.hash

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisHashTemplate(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val redisTemplate: RedisTemplate<String, String>,
) {

    fun increment(key: String, field: String, delta: Long): Long {
        val ops: HashOperations<String, String, String> = masterRedisTemplate.opsForHash()
        return ops.increment(key, field, delta)
    }

    fun entriesFromMaster(key: String): Map<String, String> {
        val ops: HashOperations<String, String, String> = masterRedisTemplate.opsForHash()
        return ops.entries(key)
    }

    fun setTtlIfAbsent(key: String, ttl: Duration) {
        val currentTtl = redisTemplate.getExpire(key) ?: -1L
        if (currentTtl == -1L) {
            masterRedisTemplate.expire(key, ttl)
        }
    }

    fun delete(key: String) {
        masterRedisTemplate.delete(key)
    }
}
