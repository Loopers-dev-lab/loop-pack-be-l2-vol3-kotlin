package com.loopers.application.consumer

import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

data class RawIntegrationEvent(
    val eventId: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val key: String,
    val version: Long,
    val occurredAt: ZonedDateTime,
    val payload: JsonNode,
)
