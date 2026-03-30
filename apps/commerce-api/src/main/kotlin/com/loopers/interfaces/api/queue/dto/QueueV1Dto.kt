package com.loopers.interfaces.api.queue.dto

import com.loopers.application.queue.QueuePositionInfo

class QueueV1Dto {

    data class QueuePositionResponse(
        val position: Long,
        val estimatedWaitSeconds: Long,
        val token: String?,
        val recommendedPollIntervalMs: Long,
    ) {
        companion object {
            fun from(info: QueuePositionInfo): QueuePositionResponse {
                return QueuePositionResponse(
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    token = info.token,
                    recommendedPollIntervalMs = info.recommendedPollIntervalMs,
                )
            }
        }
    }
}
