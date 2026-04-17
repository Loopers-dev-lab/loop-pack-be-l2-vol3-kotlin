package com.loopers.batch.job.snapshot

import java.time.LocalDate

data class RankedSnapshot(
    val productId: Long,
    val metricDate: LocalDate,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val totalScore: Double,
    val rankPosition: Int,
)
