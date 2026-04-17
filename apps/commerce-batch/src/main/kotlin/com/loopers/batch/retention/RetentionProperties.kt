package com.loopers.batch.retention

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.retention")
data class RetentionProperties(
    val cron: String,
    val dailyRetentionDays: Long,
    val weeklyRetentionWeeks: Long,
    val monthlyRetentionMonths: Long,
    val batchSize: Int,
)
