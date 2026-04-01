package com.loopers.domain.queue.waiting.model

data class QueuePosition(
    val position: Long,
    val estimatedWaitSeconds: Long,
) {
    companion object {
        fun of(position: Long, throughputTps: Int): QueuePosition {
            val safePosition = position.coerceAtLeast(0L)
            val estimatedWaitSeconds = if (throughputTps > 0) safePosition / throughputTps else 0L
            return QueuePosition(
                position = safePosition,
                estimatedWaitSeconds = estimatedWaitSeconds,
            )
        }
    }
}
