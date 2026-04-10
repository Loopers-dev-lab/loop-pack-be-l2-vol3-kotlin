package com.loopers.domain.queue

data class QueuePositionResult(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val token: String?,
)
