package com.loopers.application.event

sealed interface PaymentEvent {

    data class Completed(
        val orderId: Long,
        val userId: Long,
        val totalAmount: Long,
    ) : PaymentEvent

    data class Failed(
        val orderId: Long,
        val userId: Long,
        val reason: String,
    ) : PaymentEvent
}
