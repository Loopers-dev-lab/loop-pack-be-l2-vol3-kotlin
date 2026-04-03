package com.loopers.domain.queue

data class QueuePosition(
    val position: Long,
    val totalWaiting: Long,
    val batchSize: Int,
    val schedulerIntervalSeconds: Int,
) {
    val estimatedWaitSeconds: Long
        get() {
            if (position <= 0) return 0
            val cycles = (position + batchSize - 1) / batchSize
            return cycles * schedulerIntervalSeconds
        }

    val retryAfter: Int
        get() = when {
            position <= 0 -> 0
            position <= 100 -> 2
            position <= 500 -> 5
            else -> 10
        }
}
