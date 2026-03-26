package com.loopers.domain.payment

import java.math.BigDecimal

class PgPaymentRequest(
    val userId: Long,
    val orderId: Long,
    val cardType: String,
    val cardNo: String,
    val amount: BigDecimal,
    val callbackUrl: String,
)
