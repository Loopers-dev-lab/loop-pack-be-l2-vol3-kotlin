package com.loopers.batch.job.snapshot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.snapshot.daily")
data class DailySnapshotProperties(
    val cron: String,
    val chunkSize: Int,
    val skipLimit: Int,
    val retryLimit: Int,
    val ttlWarnThresholdHours: Long,
)
