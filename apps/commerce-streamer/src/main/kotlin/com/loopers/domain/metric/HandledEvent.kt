package com.loopers.domain.metric

import com.loopers.infrastructure.outbox.KafkaEventType

data class HandledEvent(
    val eventId: Long,
    val topic: String,
    val eventType: KafkaEventType,
)
