package com.loopers.application.queue

data class QueueEntryResult(
    val status: QueueStatus,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
) {
    enum class QueueStatus { QUEUED, ALREADY_AUTHORIZED }

    companion object {
        fun queued(position: Long, estimatedWaitSeconds: Long) =
            QueueEntryResult(QueueStatus.QUEUED, position, estimatedWaitSeconds, null)

        fun alreadyAuthorized(token: String) =
            QueueEntryResult(QueueStatus.ALREADY_AUTHORIZED, null, null, token)
    }
}
