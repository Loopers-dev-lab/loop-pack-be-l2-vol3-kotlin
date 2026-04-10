package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
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

    override fun carryOver(
        sourceDate: LocalDate,
        destinationDate: LocalDate,
        weight: Double,
    ) {
        val sourceKey = buildKey(sourceDate)
        val destKey = buildKey(destinationDate)
        val destExists = redisTemplate.hasKey(destKey) == true

        if (destExists) {
            redisTemplate.opsForZSet().unionAndStore(destKey, listOf(sourceKey), destKey, Aggregate.SUM, Weights.of(1.0, weight))
        } else {
            redisTemplate.opsForZSet().unionAndStore(sourceKey, emptyList(), destKey, Aggregate.SUM, Weights.of(weight))
        }
        redisTemplate.expire(destKey, TTL_DAYS, TimeUnit.DAYS)
    }

    override fun exists(date: LocalDate): Boolean {
        val key = buildKey(date)
        val size = redisTemplate.opsForZSet().size(key) ?: 0
        return size > 0
    }

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private const val TTL_DAYS = 2L

        fun buildKey(date: LocalDate): String =
            "$KEY_PREFIX${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }
}
