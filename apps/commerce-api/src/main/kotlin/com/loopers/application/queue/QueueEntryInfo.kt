package com.loopers.application.queue

data class QueueEntryInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val totalWaiting: Long,
)
