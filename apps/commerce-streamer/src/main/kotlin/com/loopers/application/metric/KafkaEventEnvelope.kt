package com.loopers.application.metric

import com.fasterxml.jackson.databind.JsonNode
import com.loopers.infrastructure.outbox.KafkaEventType

data class KafkaEventEnvelope(
    val eventId: Long,
    val eventType: KafkaEventType,
    val aggregateId: Long,
    val payload: JsonNode,
)
