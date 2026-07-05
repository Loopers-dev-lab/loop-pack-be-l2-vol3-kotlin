package com.loopers.application.queue

data class QueueEntryResult(
    val status: QueueStatus,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
    val totalWaiting: Long?,
) {
    enum class QueueStatus { QUEUED, ALREADY_AUTHORIZED }

    companion object {
        fun queued(position: Long, estimatedWaitSeconds: Long, totalWaiting: Long) =
            QueueEntryResult(QueueStatus.QUEUED, position, estimatedWaitSeconds, null, totalWaiting)

        fun alreadyAuthorized(token: String) =
            QueueEntryResult(QueueStatus.ALREADY_AUTHORIZED, null, null, token, null)
    }
}
