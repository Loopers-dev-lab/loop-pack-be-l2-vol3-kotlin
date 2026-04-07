package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueuePositionInfo
import com.loopers.application.queue.QueueStatus

data class QueuePositionResponse(
    val status: QueueStatus,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val suggestedPollIntervalMs: Long?,
) {
    companion object {
        fun from(info: QueuePositionInfo): QueuePositionResponse = QueuePositionResponse(
            status = info.status,
            position = info.position,
            estimatedWaitSeconds = info.estimatedWaitSeconds,
            suggestedPollIntervalMs = info.suggestedPollIntervalMs,
        )
    }
}
