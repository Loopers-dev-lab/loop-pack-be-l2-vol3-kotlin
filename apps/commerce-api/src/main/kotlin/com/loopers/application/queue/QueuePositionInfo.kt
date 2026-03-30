package com.loopers.application.queue

import com.loopers.domain.queue.waiting.model.QueuePosition

data class QueuePositionInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val token: String? = null,
) {
    companion object {
        fun from(queuePosition: QueuePosition, token: String? = null): QueuePositionInfo {
            return QueuePositionInfo(
                position = queuePosition.position,
                estimatedWaitSeconds = queuePosition.estimatedWaitSeconds,
                token = token,
            )
        }
    }
}
