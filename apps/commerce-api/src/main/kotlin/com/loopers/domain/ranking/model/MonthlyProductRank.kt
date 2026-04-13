package com.loopers.domain.ranking.model

import java.time.LocalDate

data class MonthlyProductRank(
    val rank: Int,
    val productId: Long,
    val score: Double,
    val viewCount: Long,
    val likeCount: Long,
    val salesCount: Long,
    val periodKey: String,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
)
