package com.loopers.domain.queue.dto

data class QueueEntryInfo(
    val queueName: String,
    val userId: Long,
    val position: Long,
    val estimatedWaitSeconds: Long,
)

data class QueuePositionInfo(
    val queueName: String,
    val userId: Long,
    val position: Long,
    val estimatedWaitSeconds: Long,
    val token: String? = null,
)

data class QueueStatusInfo(
    val queueName: String,
    val totalWaiting: Long,
    val throughputPerSecond: Long,
)
