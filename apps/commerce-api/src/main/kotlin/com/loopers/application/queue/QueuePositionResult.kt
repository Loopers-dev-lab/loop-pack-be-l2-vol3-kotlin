package com.loopers.application.queue

data class QueuePositionResult(
    val status: PositionStatus,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
) {
    enum class PositionStatus { WAITING, AUTHORIZED, NOT_IN_QUEUE }

    companion object {
        fun waiting(position: Long, estimatedWaitSeconds: Long) =
            QueuePositionResult(PositionStatus.WAITING, position, estimatedWaitSeconds, null)

        fun authorized(token: String) =
            QueuePositionResult(PositionStatus.AUTHORIZED, 0, 0, token)

        fun notInQueue() =
            QueuePositionResult(PositionStatus.NOT_IN_QUEUE, null, null, null)
    }
}
