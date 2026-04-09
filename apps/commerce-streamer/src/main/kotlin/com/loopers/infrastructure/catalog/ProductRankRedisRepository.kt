package com.loopers.infrastructure.catalog

import com.loopers.config.rank.ProductRankProperties
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Component
class ProductRankRedisRepository(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val properties: ProductRankProperties,
) {
    companion object {
        private const val KEY_PREFIX = "rank"
        private const val TYPE_VIEW = "view"
        private const val TYPE_LIKE = "like"
        private const val TYPE_ORDER = "order"
        private const val TYPE_ALL = "all"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun incrementView(productId: Long, date: LocalDate = LocalDate.now()) {
        increment(TYPE_VIEW, productId, 1.0, date)
        increment(TYPE_ALL, productId, properties.weight.view, date)
    }

    fun incrementLike(productId: Long, date: LocalDate = LocalDate.now()) {
        increment(TYPE_LIKE, productId, 1.0, date)
        increment(TYPE_ALL, productId, properties.weight.like, date)
    }

    fun incrementOrder(productId: Long, count: Long = 1, date: LocalDate = LocalDate.now()) {
        increment(TYPE_ORDER, productId, count.toDouble(), date)
        increment(TYPE_ALL, productId, properties.weight.order * count, date)
    }

    private fun increment(type: String, productId: Long, delta: Double, date: LocalDate) {
        val key = key(type, date)
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), delta)
        redisTemplate.expire(key, properties.ttlDays, TimeUnit.DAYS)
    }

    private fun key(type: String, date: LocalDate): String =
        "$KEY_PREFIX:$type:${date.format(DATE_FORMAT)}"
}
