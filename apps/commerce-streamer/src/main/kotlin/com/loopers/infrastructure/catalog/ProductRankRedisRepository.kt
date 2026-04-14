package com.loopers.infrastructure.catalog

import com.loopers.config.rank.ProductRankProperties
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
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

    /**
     * 오늘 rank:all의 score를 carry-over 비율만큼 내일 rank:all에 합산한다.
     * 콜드스타트 방지 — 내일 자정에 텅 빈 랭킹 대신, 오늘 인기 상품이 낮은 점수로 보임.
     *
     * 실행 시점: 23:50 (오늘 데이터가 거의 완성된 상태)
     * 공식: 내일[product] = 내일[product] × 1.0 + 오늘[product] × carryOverRatio
     */
    fun carryOver(today: LocalDate) {
        val todayKey = key(TYPE_ALL, today)
        val tomorrowKey = key(TYPE_ALL, today.plusDays(1))

        // ZUNIONSTORE dest 2 dest src WEIGHTS 1.0 {ratio} AGGREGATE SUM
        redisTemplate.opsForZSet().unionAndStore(
            tomorrowKey,
            listOf(todayKey),
            tomorrowKey,
            Aggregate.SUM,
            Weights.of(1.0, properties.carryOverRatio),
        )
        redisTemplate.expire(tomorrowKey, properties.ttlDays, TimeUnit.DAYS)
    }

    private fun increment(type: String, productId: Long, delta: Double, date: LocalDate) {
        val key = key(type, date)
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), delta)
        redisTemplate.expire(key, properties.ttlDays, TimeUnit.DAYS)
    }

    private fun key(type: String, date: LocalDate): String =
        "$KEY_PREFIX:$type:${date.format(DATE_FORMAT)}"
}
