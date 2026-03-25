package com.loopers.application.payment

object PaymentCommand {
    data class RequestPayment(
        val userId: Long,
        val orderId: Long,
        val cardType: String,
        val cardNo: String,
    ) {
        override fun toString(): String =
            "RequestPayment(userId=$userId, orderId=$orderId, cardType=$cardType, cardNo=****)"
    }

    data class HandleCallback(
        val orderId: Long,
        val transactionKey: String,
        val success: Boolean,
        val reason: String? = null,
    )
}
