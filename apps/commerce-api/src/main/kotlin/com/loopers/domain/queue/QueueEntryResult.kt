package com.loopers.domain.queue

data class QueueEntryResult(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
)
