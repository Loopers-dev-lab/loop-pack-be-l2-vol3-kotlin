package com.loopers.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "queue")
data class QueueProperties(
    val enabled: Boolean = false,
    val batchSize: Int = 30,
    val schedulerIntervalMs: Long = 200,
    val tokenTtlSeconds: Long = 300,
    val maxActiveTokens: Int = 150,
    val counterCorrectionInterval: Int = 50,
)
