package com.loopers.application.payment

import com.loopers.domain.payment.CardType

class PaymentCommand {
    data class RequestPayment(
        val orderId: Long,
        val cardType: CardType,
        val cardNo: String,
        val amount: Long,
    )
}
