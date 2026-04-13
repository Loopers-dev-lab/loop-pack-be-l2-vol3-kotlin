package com.loopers.infrastructure.outbox

data class PaymentSucceededOutboxMessagePayload(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val items: List<PaymentSucceededOutboxMessageItemPayload>,
)

data class PaymentSucceededOutboxMessageItemPayload(
    val productId: Long,
    val quantity: Int,
    val sellingPrice: Long,
)
