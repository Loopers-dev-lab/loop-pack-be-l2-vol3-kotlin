package com.loopers.application.queue

import com.loopers.domain.queue.QueueEntryInfo
import com.loopers.domain.queue.QueuePositionInfo

data class QueueEntryResult(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val retryAfterMs: Long,
    val alreadyInQueue: Boolean,
) {
    companion object {
        fun from(info: QueueEntryInfo): QueueEntryResult {
            return QueueEntryResult(
                position = info.position,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                retryAfterMs = info.retryAfterMs,
                alreadyInQueue = info.alreadyInQueue,
            )
        }
    }
}

data class QueueStatusResult(
    val enabled: Boolean,
)

data class QueuePositionResult(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val retryAfterMs: Long,
    val token: String?,
    val totalWaiting: Long,
) {
    companion object {
        fun from(info: QueuePositionInfo): QueuePositionResult {
            return QueuePositionResult(
                position = info.position,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                retryAfterMs = info.retryAfterMs,
                token = info.token,
                totalWaiting = info.totalWaiting,
            )
        }
    }
}
