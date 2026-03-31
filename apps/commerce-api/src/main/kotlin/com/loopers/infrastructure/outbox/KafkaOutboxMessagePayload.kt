package com.loopers.infrastructure.outbox

data class KafkaOutboxMessagePayload(
    val productId: Long,
    val userId: Long? = null,
)
