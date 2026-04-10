package com.loopers.application.queue

data class QueuePositionInfo(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val token: String?,
)
