package com.loopers.application.queue

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
    val schedulerDelayMs: Long,
) {
    val throughputTps: Int get() = (batchSize * 1000 / schedulerDelayMs).toInt()
}
