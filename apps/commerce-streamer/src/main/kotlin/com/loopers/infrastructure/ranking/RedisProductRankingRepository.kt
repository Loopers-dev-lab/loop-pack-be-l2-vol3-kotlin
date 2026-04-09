package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Component
class RedisProductRankingRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : ProductRankingRepository {
    override fun incrementScore(
        productId: Long,
        increment: Double,
    ) {
        val key = buildKey(LocalDate.now(ZoneId.of("Asia/Seoul")))
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), increment)
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS)
    }

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private const val TTL_DAYS = 2L

        fun buildKey(date: LocalDate): String =
            "$KEY_PREFIX${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }
}
