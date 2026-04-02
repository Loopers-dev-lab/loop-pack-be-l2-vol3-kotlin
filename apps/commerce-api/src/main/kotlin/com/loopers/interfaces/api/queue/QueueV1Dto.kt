package com.loopers.interfaces.api.queue

import com.loopers.application.queue.EnterQueueCriteria
import com.loopers.application.queue.QueueEntryResult
import com.loopers.application.queue.QueuePositionResult

class QueueV1Dto {

    data class EnterQueueResponse(
        val position: Long,
        val estimatedWaitSeconds: Long,
        val retryAfterMs: Long,
        val alreadyInQueue: Boolean,
    ) {
        companion object {
            fun from(result: QueueEntryResult): EnterQueueResponse {
                return EnterQueueResponse(
                    position = result.position,
                    estimatedWaitSeconds = result.estimatedWaitSeconds,
                    retryAfterMs = result.retryAfterMs,
                    alreadyInQueue = result.alreadyInQueue,
                )
            }
        }
    }

    data class QueuePositionResponse(
        val position: Long,
        val estimatedWaitSeconds: Long,
        val retryAfterMs: Long,
        val token: String?,
        val totalWaiting: Long,
    ) {
        companion object {
            fun from(result: QueuePositionResult): QueuePositionResponse {
                return QueuePositionResponse(
                    position = result.position,
                    estimatedWaitSeconds = result.estimatedWaitSeconds,
                    retryAfterMs = result.retryAfterMs,
                    token = result.token,
                    totalWaiting = result.totalWaiting,
                )
            }
        }
    }
}
