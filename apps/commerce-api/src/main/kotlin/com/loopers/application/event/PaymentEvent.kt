package com.loopers.application.event

sealed interface PaymentEvent {

    data class Completed(
        val orderId: Long,
        val userId: Long,
        val totalAmount: Long,
        val items: List<OrderedProduct>,
    ) : PaymentEvent {
        data class OrderedProduct(
            val productId: Long,
            val quantity: Int,
        )
    }

    data class Failed(
        val orderId: Long,
        val userId: Long,
        val reason: String,
    ) : PaymentEvent
}
