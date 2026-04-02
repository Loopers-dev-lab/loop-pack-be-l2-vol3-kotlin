package com.loopers.application.queue

data class QueuePositionInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val totalWaiting: Long,
    val token: String?,
)
