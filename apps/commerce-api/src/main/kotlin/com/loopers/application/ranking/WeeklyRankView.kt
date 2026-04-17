package com.loopers.application.ranking

import java.time.LocalDate

data class WeeklyRankView(
    val productId: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val totalScore: Double,
    val rankPosition: Int,
)
