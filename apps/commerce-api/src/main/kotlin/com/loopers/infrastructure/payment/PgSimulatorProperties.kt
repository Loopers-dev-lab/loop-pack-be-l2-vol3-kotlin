package com.loopers.infrastructure.payment

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "pg-simulator")
data class PgSimulatorProperties(
    val baseUrl: String,
    val callbackUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
)
