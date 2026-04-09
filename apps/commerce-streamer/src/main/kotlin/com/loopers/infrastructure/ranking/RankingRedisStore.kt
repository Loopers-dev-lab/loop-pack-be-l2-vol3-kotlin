package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RankingRedisStore(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private val KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val KEY_TTL: Duration = Duration.ofDays(2)
    }

    fun incrementScore(productId: Long, occurredAt: ZonedDateTime, score: Double) {
        val key = "ranking:all:${KEY_FORMATTER.format(occurredAt)}"
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        redisTemplate.expire(key, KEY_TTL)
    }
}
