package com.loopers.infrastructure.orderqueue

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "order-queue")
data class OrderQueueProperties(
    val throughputPerSecond: Long = 70L,
    val tokenTtlSeconds: Long = 300L,
    val scheduler: SchedulerProperties = SchedulerProperties(),
    val rateLimit: RateLimitProperties = RateLimitProperties(),
) {
    data class SchedulerProperties(
        val batchSize: Long = 7L,
    )

    data class RateLimitProperties(
        val maxRequests: Long = 100L,
        val windowSeconds: Long = 1L,
    )
}
