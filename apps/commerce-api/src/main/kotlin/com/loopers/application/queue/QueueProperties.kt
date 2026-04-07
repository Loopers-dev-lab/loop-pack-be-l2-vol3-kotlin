package com.loopers.application.queue

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "queue")
data class QueueProperties(
    val fallbackStrategy: QueueFallbackStrategy = QueueFallbackStrategy.BLOCK,
)
