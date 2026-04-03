package com.loopers.domain.queue

data class QueuePosition(
    val position: Long,
    val estimatedWaitSeconds: Double,
    val totalSize: Long,
    val token: String? = null,
)
