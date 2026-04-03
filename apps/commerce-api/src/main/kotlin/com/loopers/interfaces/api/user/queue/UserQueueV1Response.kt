package com.loopers.interfaces.api.user.queue

import com.loopers.application.user.queue.QueueResult

class UserQueueV1Response {
    data class Enter(
        val status: String,
        val position: Long?,
        val estimatedWaitSeconds: Long?,
        val totalWaiting: Long?,
        val token: String?,
        val tokenExpiresInSeconds: Long?,
    ) {
        companion object {
            fun from(result: QueueResult.Enter): Enter =
                when (result) {
                    is QueueResult.Enter.Waiting ->
                        Enter(
                            status = QueueResult.QueueStatus.WAITING.name,
                            position = result.position,
                            estimatedWaitSeconds = result.estimatedWaitSeconds,
                            totalWaiting = result.totalWaiting,
                            token = null,
                            tokenExpiresInSeconds = null,
                        )
                    is QueueResult.Enter.Ready ->
                        Enter(
                            status = QueueResult.QueueStatus.READY.name,
                            position = null,
                            estimatedWaitSeconds = null,
                            totalWaiting = null,
                            token = result.token,
                            tokenExpiresInSeconds = result.tokenExpiresInSeconds,
                        )
                }
        }
    }

    data class Position(
        val status: String,
        val position: Long?,
        val estimatedWaitSeconds: Long?,
        val totalWaiting: Long?,
        val retryAfterMs: Long?,
        val tokenExpiresInSeconds: Long?,
    ) {
        companion object {
            fun from(result: QueueResult.Position): Position =
                when (result) {
                    is QueueResult.Position.Waiting ->
                        Position(
                            status = QueueResult.QueueStatus.WAITING.name,
                            position = result.position,
                            estimatedWaitSeconds = result.estimatedWaitSeconds,
                            totalWaiting = result.totalWaiting,
                            retryAfterMs = result.retryAfterMs,
                            tokenExpiresInSeconds = null,
                        )
                    is QueueResult.Position.Ready ->
                        Position(
                            status = QueueResult.QueueStatus.READY.name,
                            position = null,
                            estimatedWaitSeconds = null,
                            totalWaiting = null,
                            retryAfterMs = null,
                            tokenExpiresInSeconds = result.tokenExpiresInSeconds,
                        )
                }
        }
    }
}
