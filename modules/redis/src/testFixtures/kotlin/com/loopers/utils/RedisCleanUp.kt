package com.loopers.utils

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class RedisCleanUp(
    private val redisConnectionFactory: RedisConnectionFactory,
) {
    fun truncateAll() {
        try {
            redisConnectionFactory.connection.use { connection ->
                connection.serverCommands().flushDb()
            }
        } catch (e: Exception) {
            // Redis 연결 실패 시 테스트 격리가 깨질 수 있으므로 명시적 로깅
            throw IllegalStateException("Redis flushDb 실패 — 테스트 격리가 보장되지 않습니다", e)
        }
    }
}
