package com.loopers.interfaces.consumer.dto

data class CatalogEventPayload(
    val eventId: String,
    val eventType: String,
    val productId: Long,
)
