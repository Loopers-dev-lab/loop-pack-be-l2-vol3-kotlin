package com.loopers.interfaces.api.v1.queue

import com.loopers.application.queue.QueueEntryResult

data class QueueEnterResponse(
    val status: String,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
) {
    companion object {
        fun from(result: QueueEntryResult) = QueueEnterResponse(
            status = result.status.name,
            position = result.position,
            estimatedWaitSeconds = result.estimatedWaitSeconds,
            token = result.token,
        )
    }
}
