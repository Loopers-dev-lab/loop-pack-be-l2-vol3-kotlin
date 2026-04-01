package com.loopers.application.queue

import com.loopers.domain.queue.waiting.model.QueuePosition

data class QueuePositionInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val token: String? = null,
    val recommendedPollIntervalMs: Long = calculatePollIntervalMs(position, token),
) {
    companion object {
        fun calculatePollIntervalMs(position: Long, token: String? = null): Long {
            if (token != null) return 0
            return when {
                position > 1000 -> 5000
                position > 100 -> 3000
                else -> 1000
            }
        }

        fun from(queuePosition: QueuePosition, token: String? = null): QueuePositionInfo {
            return QueuePositionInfo(
                position = queuePosition.position,
                estimatedWaitSeconds = queuePosition.estimatedWaitSeconds,
                token = token,
            )
        }
    }
}
