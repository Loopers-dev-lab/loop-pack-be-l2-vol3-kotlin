package com.loopers.interfaces.consumer.dto

data class OrderEventPayload(
    val eventId: String,
    val eventType: String,
    val productId: Long,
    val quantity: Long? = null,
)
