package com.loopers.interfaces.api.orderqueue

import com.loopers.application.orderqueue.EnterQueueResult
import com.loopers.application.orderqueue.GetQueuePositionResult
import com.loopers.domain.orderqueue.QueueStatus

class OrderQueueV1Dto {
    data class EnterResponse(
        val position: Long,
        val totalWaiting: Long,
        val estimatedWaitSeconds: Long,
        val pollingIntervalSeconds: Int,
    ) {
        companion object {
            fun from(result: EnterQueueResult): EnterResponse {
                return EnterResponse(
                    position = result.position,
                    totalWaiting = result.totalWaiting,
                    estimatedWaitSeconds = result.estimatedWaitSeconds,
                    pollingIntervalSeconds = result.pollingIntervalSeconds,
                )
            }
        }
    }

    data class PositionResponse(
        val status: QueueStatus,
        val position: Long? = null,
        val totalWaiting: Long? = null,
        val estimatedWaitSeconds: Long? = null,
        val pollingIntervalSeconds: Int? = null,
        val tokenExpireSeconds: Long? = null,
    ) {
        companion object {
            fun from(result: GetQueuePositionResult): PositionResponse {
                return PositionResponse(
                    status = result.status,
                    position = result.position,
                    totalWaiting = result.totalWaiting,
                    estimatedWaitSeconds = result.estimatedWaitSeconds,
                    pollingIntervalSeconds = result.pollingIntervalSeconds,
                    tokenExpireSeconds = result.tokenExpireSeconds,
                )
            }
        }
    }
}
