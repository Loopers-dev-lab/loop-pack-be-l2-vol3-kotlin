package com.loopers.batch.consistency

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.consistency")
data class ConsistencyCheckProperties(
    val cron: String,
    val warnThreshold: Int,
)
