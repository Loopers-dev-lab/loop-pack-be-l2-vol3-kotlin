package com.loopers.application.user.queue

object QueueCalculator {
    const val THROUGHPUT_PER_SECOND = 140L
    const val TOKEN_EXPIRY_SECONDS = 300L

    fun estimateWaitSeconds(position: Long): Long =
        if (position <= 0) 0 else (position + THROUGHPUT_PER_SECOND - 1) / THROUGHPUT_PER_SECOND

    fun calculateRetryAfterMs(position: Long): Long =
        when {
            position <= 100 -> 1000L
            position <= 1000 -> 3000L
            else -> 5000L
        }
}
