package com.loopers.application.queue

data class QueueEntryInfo(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
)
