package com.loopers.application.queue

import com.loopers.domain.queue.QueuePosition

data class QueuePositionInfo(
    val position: Long,
    val estimatedWaitSeconds: Double,
    val totalSize: Long,
    val token: String? = null,
    val pollingIntervalMs: Long,
) {
    companion object {
        fun from(queuePosition: QueuePosition, pollingIntervalMs: Long): QueuePositionInfo {
            return QueuePositionInfo(
                position = queuePosition.position,
                estimatedWaitSeconds = queuePosition.estimatedWaitSeconds,
                totalSize = queuePosition.totalSize,
                token = queuePosition.token,
                pollingIntervalMs = pollingIntervalMs,
            )
        }
    }
}
