package com.loopers.interfaces.support.config

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "queue")
data class QueueProperties(
    @field:Positive
    val maxCapacity: Int,
    @field:Positive
    val batchSize: Int,
    @field:Positive
    val tokenTtlSeconds: Long,
    @field:Positive
    val throughputTps: Int,
    @field:Positive
    val schedulerDelayMs: Long,
)
