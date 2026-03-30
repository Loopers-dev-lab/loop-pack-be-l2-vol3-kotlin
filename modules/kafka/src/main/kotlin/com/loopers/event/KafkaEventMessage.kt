package com.loopers.event

import java.time.ZonedDateTime

data class KafkaEventMessage(
    val eventId: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val payload: Map<String, Any?>,
    val version: Long,
    val occurredAt: ZonedDateTime,
)
