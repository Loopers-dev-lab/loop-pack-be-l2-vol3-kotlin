package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType

class QueueV1Dto {
    data class EnterRequest(
        val strategy: QueueStrategyType? = null,
    )

    data class PositionResponse(
        val strategy: QueueStrategyType,
        val state: String,
        val position: Long?,
        val totalWaitingCount: Long,
        val expectedWaitSeconds: Long,
        val canEnterOrderApi: Boolean,
        val token: String?,
        val tokenExpiresAt: String?,
    ) {
        companion object {
            fun from(status: QueueInfo.Status): PositionResponse {
                return PositionResponse(
                    strategy = status.strategy,
                    state = status.state.name,
                    position = status.position,
                    totalWaitingCount = status.totalWaitingCount,
                    expectedWaitSeconds = status.expectedWaitSeconds,
                    canEnterOrderApi = status.canEnterOrderApi,
                    token = status.token,
                    tokenExpiresAt = status.tokenExpiresAt?.toString(),
                )
            }
        }
    }
}
