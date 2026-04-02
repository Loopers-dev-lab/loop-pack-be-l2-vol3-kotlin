package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueEntryInfo
import com.loopers.application.queue.QueuePositionInfo

class QueueV1Dto {
    data class EnterResponse(
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
    ) {
        companion object {
            fun from(info: QueueEntryInfo): EnterResponse {
                return EnterResponse(
                    position = info.position,
                    totalWaiting = info.totalWaiting,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                )
            }
        }
    }

    data class PositionResponse(
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
        val token: String?,
    ) {
        companion object {
            fun from(info: QueuePositionInfo): PositionResponse {
                return PositionResponse(
                    position = info.position,
                    totalWaiting = info.totalWaiting,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    token = info.token,
                )
            }
        }
    }
}
