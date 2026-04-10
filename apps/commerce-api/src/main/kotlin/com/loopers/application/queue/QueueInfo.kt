package com.loopers.application.queue

import com.loopers.domain.queue.QueuePosition

data class QueueInfo(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val retryAfter: Int,
    val token: String?,
) {
    companion object {
        fun from(queuePosition: QueuePosition, token: String? = null): QueueInfo {
            return QueueInfo(
                position = queuePosition.position,
                totalWaiting = queuePosition.totalWaiting,
                estimatedWaitSeconds = queuePosition.estimatedWaitSeconds,
                retryAfter = queuePosition.retryAfter,
                token = token,
            )
        }
    }
}

data class QueueStatusInfo(
    val enabled: Boolean,
    val totalWaiting: Long,
    val activeTokens: Long,
)
