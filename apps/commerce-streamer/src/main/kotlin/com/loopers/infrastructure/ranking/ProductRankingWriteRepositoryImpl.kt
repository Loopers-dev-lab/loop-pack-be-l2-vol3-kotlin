package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingWriteRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ProductRankingWriteRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : ProductRankingWriteRepository {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
        private val TTL: Duration = Duration.ofDays(2)
    }

    override fun incrementScore(processingDate: LocalDate, productId: Long, score: Double) {
        val key = key(processingDate)
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
        val ttl = redisTemplate.getExpire(key)
        if (ttl == null || ttl < 0) {
            redisTemplate.expire(key, TTL)
        }
    }

    private fun key(processingDate: LocalDate): String = "$KEY_PREFIX${processingDate.format(DATE_FORMATTER)}"
}
