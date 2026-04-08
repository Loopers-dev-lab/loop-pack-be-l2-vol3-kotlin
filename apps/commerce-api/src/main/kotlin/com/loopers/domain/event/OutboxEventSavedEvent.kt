package com.loopers.domain.event

data class OutboxEventSavedEvent(
    val outboxEventId: Long,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val topic: String,
)
