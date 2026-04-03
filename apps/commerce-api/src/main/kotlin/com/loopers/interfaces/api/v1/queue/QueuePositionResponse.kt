package com.loopers.interfaces.api.v1.queue

import com.loopers.application.queue.QueuePositionResult

data class QueuePositionResponse(
    val status: String,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
) {
    companion object {
        fun from(result: QueuePositionResult) = QueuePositionResponse(
            status = result.status.name,
            position = result.position,
            estimatedWaitSeconds = result.estimatedWaitSeconds,
            token = result.token,
        )
    }
}
