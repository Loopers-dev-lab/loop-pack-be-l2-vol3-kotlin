package com.loopers.utils

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class RedisCleanUp(
    private val redisConnectionFactory: RedisConnectionFactory,
) {
    fun truncateAll() {
        runCatching {
            redisConnectionFactory.connection.use { it.serverCommands().flushAll() }
        }.onFailure { ex ->
            System.err.println("Warning: Failed to flush Redis: ${ex.message}")
        }
    }
}
