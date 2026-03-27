package com.loopers.domain.common.outbox

import java.time.ZonedDateTime

data class OutboxEvent(
    val id: String,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val partitionKey: String,
    val topic: String,
    val createdAt: ZonedDateTime? = null,
    val publishedAt: ZonedDateTime? = null,
)
