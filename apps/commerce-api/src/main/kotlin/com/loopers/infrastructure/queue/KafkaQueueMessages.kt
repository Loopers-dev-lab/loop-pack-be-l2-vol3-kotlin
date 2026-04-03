package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueStrategyType

data class KafkaQueueEnterMessage(
    val strategy: QueueStrategyType,
    val memberId: Long,
)

data class KafkaQueueAdmissionMessage(
    val strategy: QueueStrategyType,
    val memberId: Long,
    val token: String,
    val expiresAt: String,
)
