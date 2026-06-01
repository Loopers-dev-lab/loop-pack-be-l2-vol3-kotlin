package com.loopers.domain.ranking

import java.time.LocalDate

data class ProductRankMvRow(
    val productId: Long,
    val totalScore: Double,
    val viewCount: Int,
    val likeCount: Int,
    val orderCount: Int,
    val rank: Int,
)

interface ProductRankMvRepository {
    fun findWeeklyByPeriodStartDate(periodStartDate: LocalDate): List<ProductRankMvRow>
    fun findMonthlyByPeriodStartDate(periodStartDate: LocalDate): List<ProductRankMvRow>
}
