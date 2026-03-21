package com.loopers.domain.payment

import com.loopers.domain.payment.model.CardType

data class PgPaymentRequest(
    val orderId: Long,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
) {
    override fun toString(): String =
        "PgPaymentRequest(orderId=$orderId, cardType=$cardType, cardNo=****, amount=$amount)"
}
