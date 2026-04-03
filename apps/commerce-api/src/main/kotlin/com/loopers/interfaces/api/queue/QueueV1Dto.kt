package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueInfo

class QueueV1Dto {

    data class EnterResponse(
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
        val retryAfter: Int,
        val token: String?,
    ) {
        companion object {
            fun from(info: QueueInfo) = EnterResponse(
                position = info.position,
                totalWaiting = info.totalWaiting,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                retryAfter = info.retryAfter,
                token = info.token,
            )
        }
    }

    data class PositionResponse(
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
        val retryAfter: Int,
        val token: String?,
    ) {
        companion object {
            fun from(info: QueueInfo) = PositionResponse(
                position = info.position,
                totalWaiting = info.totalWaiting,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                retryAfter = info.retryAfter,
                token = info.token,
            )
        }
    }
}
