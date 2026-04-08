package com.loopers.domain.queue

data class QueuePositionInfo(
    val status: QueueStatus,
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val token: String? = null,
)
