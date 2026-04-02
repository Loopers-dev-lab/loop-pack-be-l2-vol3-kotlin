package com.loopers.application.queue

data class QueueEntryInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val alreadyHasToken: Boolean,
)

data class QueuePositionInfo(
    val status: QueueStatus,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val suggestedPollIntervalMs: Long?,
) {
    companion object {
        fun ready(): QueuePositionInfo = QueuePositionInfo(
            status = QueueStatus.READY,
            position = null,
            estimatedWaitSeconds = null,
            suggestedPollIntervalMs = null,
        )

        fun waiting(
            position: Long,
            estimatedWaitSeconds: Long,
            suggestedPollIntervalMs: Long,
        ): QueuePositionInfo = QueuePositionInfo(
            status = QueueStatus.WAITING,
            position = position,
            estimatedWaitSeconds = estimatedWaitSeconds,
            suggestedPollIntervalMs = suggestedPollIntervalMs,
        )

        fun notInQueue(): QueuePositionInfo = QueuePositionInfo(
            status = QueueStatus.NOT_IN_QUEUE,
            position = null,
            estimatedWaitSeconds = null,
            suggestedPollIntervalMs = null,
        )
    }
}

enum class QueueStatus {
    READY,
    WAITING,
    NOT_IN_QUEUE,
}
