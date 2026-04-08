package com.loopers.application.ranking

import com.loopers.zset.RankingKeyGenerator
import com.loopers.zset.RedisZSetTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import kotlin.math.log10

@Component
class RankingScoreAccumulator(
    private val redisZSetTemplate: RedisZSetTemplate,
    private val rankingProperties: RankingProperties,
) {

    fun addLikeScore(productId: Long) {
        val key = RankingKeyGenerator.todayKey()
        redisZSetTemplate.incrementScore(key, productId.toString(), rankingProperties.weight.like)
        ensureTtl(key)
    }

    fun cancelLikeScore(productId: Long) {
        val key = RankingKeyGenerator.todayKey()
        redisZSetTemplate.incrementScore(key, productId.toString(), -rankingProperties.weight.like)
    }

    fun addOrderScore(productIds: List<Long>, totalAmount: Long) {
        if (productIds.isEmpty()) return

        val key = RankingKeyGenerator.todayKey()
        val revenuePerProduct = totalAmount / productIds.size
        val increment = rankingProperties.weight.order * log10(1.0 + revenuePerProduct)

        productIds.forEach { productId ->
            redisZSetTemplate.incrementScore(key, productId.toString(), increment)
        }
        ensureTtl(key)
    }

    fun addPaymentScore(productIds: List<Long>, amount: Long) {
        if (productIds.isEmpty()) return

        val key = RankingKeyGenerator.todayKey()
        val revenuePerProduct = amount / productIds.size
        val increment = rankingProperties.weight.order * log10(1.0 + revenuePerProduct)

        productIds.forEach { productId ->
            redisZSetTemplate.incrementScore(key, productId.toString(), increment)
        }
        ensureTtl(key)
    }

    fun addViewScore(productId: Long, count: Long) {
        if (count <= 0) return

        val key = RankingKeyGenerator.todayKey()
        val increment = rankingProperties.weight.view * count
        redisZSetTemplate.incrementScore(key, productId.toString(), increment)
        ensureTtl(key)
    }

    private fun ensureTtl(key: String) {
        redisZSetTemplate.setTtlIfAbsent(key, Duration.ofDays(rankingProperties.ttlDays))
    }
}
