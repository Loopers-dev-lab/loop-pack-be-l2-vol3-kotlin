package com.loopers.domain.queue

data class QueueEntryInfo(
    val position: Long,
    val totalWaiting: Long,
)
