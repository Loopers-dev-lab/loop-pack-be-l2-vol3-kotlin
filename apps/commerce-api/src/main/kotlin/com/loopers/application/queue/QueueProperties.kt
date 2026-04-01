package com.loopers.application.queue

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
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
    @field:PositiveOrZero
    val jitterMaxMs: Long = 0,
)
