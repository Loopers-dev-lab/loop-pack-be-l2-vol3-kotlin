package com.loopers.application.user.queue

class QueueResult {
    enum class QueueStatus {
        WAITING,
        READY,
    }

    sealed interface Enter {
        data class Waiting(
            val position: Long,
            val estimatedWaitSeconds: Long,
            val totalWaiting: Long,
        ) : Enter

        data class Ready(
            val token: String,
            val tokenExpiresInSeconds: Long,
        ) : Enter
    }

    sealed interface Position {
        data class Waiting(
            val position: Long,
            val estimatedWaitSeconds: Long,
            val totalWaiting: Long,
            val retryAfterMs: Long,
        ) : Position

        data class Ready(
            val tokenExpiresInSeconds: Long,
        ) : Position
    }
}
