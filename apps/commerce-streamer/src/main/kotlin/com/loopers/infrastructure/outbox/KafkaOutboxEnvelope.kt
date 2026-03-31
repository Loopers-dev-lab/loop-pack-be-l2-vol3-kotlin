package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.JsonNode

data class KafkaOutboxEnvelope(
    val eventId: Long,
    val eventType: KafkaEventType,
    val aggregateId: Long,
    val payload: JsonNode,
)
