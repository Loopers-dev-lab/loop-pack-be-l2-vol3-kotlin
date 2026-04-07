package com.loopers.application.queue

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "queue")
data class QueueProperties(
    val scheduler: Scheduler = Scheduler(),
    val token: Token = Token(),
    val redis: RedisKeys = RedisKeys(),
    val maxQueueSize: Long = 50000,
    val enabled: Boolean = true,
) {
    data class Scheduler(
        val intervalMs: Long = 100,
        val batchSize: Long = 5,
    )

    data class Token(
        val ttlSeconds: Long = 300,
    )

    data class RedisKeys(
        val queueKey: String = "waiting-queue",
        val tokenKeyPrefix: String = "entry-token:",
    )
}
