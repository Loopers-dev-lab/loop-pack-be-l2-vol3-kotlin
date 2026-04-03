package com.loopers.application.queue

object QueueCalculator {

    fun estimatedWaitSeconds(position: Long, batchSize: Long, intervalMs: Long): Long {
        val batchesPerSecond = 1000.0 / intervalMs
        val throughputPerSecond = batchSize * batchesPerSecond
        return if (throughputPerSecond > 0) {
            (position / throughputPerSecond).toLong()
        } else {
            0L
        }
    }

    fun suggestedPollIntervalMs(position: Long): Long {
        return when {
            position <= 100 -> 1000L
            position <= 1000 -> 3000L
            else -> 5000L
        }
    }
}
