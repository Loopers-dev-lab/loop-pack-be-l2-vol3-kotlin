package com.loopers.interfaces.api.queue

import com.loopers.domain.queue.QueueEntryInfo
import com.loopers.domain.queue.QueuePositionInfo
import com.loopers.domain.queue.QueueStatus

class QueueV1Dto {
    data class EnterResponse(
        val position: Long,
        val totalWaiting: Long,
    ) {
        companion object {
            fun from(info: QueueEntryInfo) = EnterResponse(
                position = info.position,
                totalWaiting = info.totalWaiting,
            )
        }
    }

    data class PositionResponse(
        val status: QueueStatus,
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
        val token: String? = null,
    ) {
        companion object {
            fun from(info: QueuePositionInfo) = PositionResponse(
                status = info.status,
                position = info.position,
                totalWaiting = info.totalWaiting,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                token = info.token,
            )
        }
    }
}
