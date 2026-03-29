package com.loopers.kafka

import java.time.ZonedDateTime

data class IntegrationEvent<T>(
    val eventId: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val key: String,
    val version: Long,
    val occurredAt: ZonedDateTime,
    val payload: T,
)
