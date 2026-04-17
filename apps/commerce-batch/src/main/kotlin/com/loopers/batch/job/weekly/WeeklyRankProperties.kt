package com.loopers.batch.job.weekly

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.ranking.weekly")
data class WeeklyRankProperties(
    val chunkSize: Int,
    val skipLimit: Int,
    val retryLimit: Int,
    val topN: Int,
)
