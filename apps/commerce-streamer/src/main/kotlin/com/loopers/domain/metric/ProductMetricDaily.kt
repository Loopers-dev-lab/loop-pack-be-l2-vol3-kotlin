package com.loopers.domain.metric

import com.loopers.domain.ranking.RankingScorePolicy
import java.time.LocalDate

class ProductMetricDaily private constructor(
    val productId: Long,
    val metricDate: LocalDate,
    val viewCount: Int,
    val likeCount: Int,
    val unitsSold: Int,
    val salesAmount: Long,
    val orderScore: Double,
) {
    init {
        require(viewCount >= 0) { "viewCount must be non-negative: $viewCount" }
        require(likeCount >= 0) { "likeCount must be non-negative: $likeCount" }
        require(unitsSold >= 0) { "unitsSold must be non-negative: $unitsSold" }
        require(salesAmount >= 0) { "salesAmount must be non-negative: $salesAmount" }
        require(orderScore >= 0.0) { "orderScore must be non-negative: $orderScore" }
    }

    fun recordView(): ProductMetricDaily = copy(viewCount = viewCount + 1)

    fun recordLike(): ProductMetricDaily = copy(likeCount = likeCount + 1)

    fun recordOrder(quantity: Int, amount: Long): ProductMetricDaily {
        require(quantity >= 0) { "quantity must be non-negative: $quantity" }
        require(amount >= 0) { "amount must be non-negative: $amount" }
        return copy(
            unitsSold = unitsSold + quantity,
            salesAmount = salesAmount + amount,
            orderScore = orderScore + RankingScorePolicy().calculateOrderIncrement(amount),
        )
    }

    private fun copy(
        viewCount: Int = this.viewCount,
        likeCount: Int = this.likeCount,
        unitsSold: Int = this.unitsSold,
        salesAmount: Long = this.salesAmount,
        orderScore: Double = this.orderScore,
    ): ProductMetricDaily = ProductMetricDaily(
        productId = productId,
        metricDate = metricDate,
        viewCount = viewCount,
        likeCount = likeCount,
        unitsSold = unitsSold,
        salesAmount = salesAmount,
        orderScore = orderScore,
    )

    companion object {
        fun register(productId: Long, metricDate: LocalDate): ProductMetricDaily = ProductMetricDaily(
            productId = productId,
            metricDate = metricDate,
            viewCount = 0,
            likeCount = 0,
            unitsSold = 0,
            salesAmount = 0L,
            orderScore = 0.0,
        )

        fun retrieve(
            productId: Long,
            metricDate: LocalDate,
            viewCount: Int,
            likeCount: Int,
            unitsSold: Int,
            salesAmount: Long,
            orderScore: Double,
        ): ProductMetricDaily = ProductMetricDaily(
            productId = productId,
            metricDate = metricDate,
            viewCount = viewCount,
            likeCount = likeCount,
            unitsSold = unitsSold,
            salesAmount = salesAmount,
            orderScore = orderScore,
        )
    }
}
