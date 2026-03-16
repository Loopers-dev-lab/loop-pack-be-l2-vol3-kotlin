package com.loopers.domain.payment

import com.loopers.domain.payment.model.CardType

data class PgPaymentRequest(
    val orderId: Long,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
)
