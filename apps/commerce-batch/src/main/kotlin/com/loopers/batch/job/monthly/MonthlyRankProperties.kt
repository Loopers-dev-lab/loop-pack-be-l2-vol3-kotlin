package com.loopers.batch.job.monthly

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.ranking.monthly")
data class MonthlyRankProperties(
    val chunkSize: Int,
    val skipLimit: Int,
    val retryLimit: Int,
    val topN: Int,
)
