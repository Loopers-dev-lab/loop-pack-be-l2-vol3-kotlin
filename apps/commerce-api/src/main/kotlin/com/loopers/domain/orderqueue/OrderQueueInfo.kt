package com.loopers.domain.orderqueue

enum class QueueStatus {
    WAITING,
    ACTIVE,
    NOT_IN_QUEUE,
}

data class QueueEntryInfo(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val pollingIntervalSeconds: Int,
)

data class QueuePositionInfo(
    val status: QueueStatus,
    val position: Long? = null,
    val totalWaiting: Long? = null,
    val estimatedWaitSeconds: Long? = null,
    val pollingIntervalSeconds: Int? = null,
    val tokenExpireSeconds: Long? = null,
)
