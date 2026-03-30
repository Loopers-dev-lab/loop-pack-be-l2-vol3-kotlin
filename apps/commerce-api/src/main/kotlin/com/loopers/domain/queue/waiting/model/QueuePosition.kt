package com.loopers.domain.queue.waiting.model

data class QueuePosition(
    val position: Long,
    val estimatedWaitSeconds: Long,
) {
    companion object {
        fun of(position: Long, throughputTps: Int): QueuePosition {
            val estimatedWaitSeconds = if (throughputTps > 0) position / throughputTps else 0L
            return QueuePosition(
                position = position,
                estimatedWaitSeconds = estimatedWaitSeconds,
            )
        }
    }
}
