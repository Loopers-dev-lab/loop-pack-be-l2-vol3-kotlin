package com.loopers.application.queue

import com.loopers.domain.queue.waiting.model.QueuePosition

data class QueuePositionInfo(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val token: String? = null,
    val recommendedPollIntervalMs: Long = calculatePollIntervalMs(position, token),
) {
    companion object {
        fun calculatePollIntervalMs(position: Long, token: String? = null): Long {
            if (token != null) return 0
            val baseMs = when {
                position > 500 -> 3000L
                position > 50 -> 2000L
                else -> 1000L
            }
            return applyJitter(baseMs)
        }

        private fun applyJitter(baseMs: Long): Long {
            val jitterRange = (baseMs * 0.2).toLong()
            val jitter = (Math.random() * 2 * jitterRange - jitterRange).toLong()
            return baseMs + jitter
        }

        fun fromToken(token: String): QueuePositionInfo {
            return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                token = token,
                recommendedPollIntervalMs = 0,
            )
        }

        fun from(queuePosition: QueuePosition, token: String? = null): QueuePositionInfo {
            if (token != null) {
                return fromToken(token)
            }
            return QueuePositionInfo(
                position = queuePosition.position,
                estimatedWaitSeconds = queuePosition.estimatedWaitSeconds,
            )
        }
    }
}
