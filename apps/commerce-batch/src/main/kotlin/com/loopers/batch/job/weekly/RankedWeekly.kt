package com.loopers.batch.job.weekly

import java.time.LocalDate

data class RankedWeekly(
    val productId: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val totalScore: Double,
    val rankPosition: Int,
)
