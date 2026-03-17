package com.loopers.domain.payment.dto

import java.math.BigDecimal

data class CreatePaymentCommand(
    val orderId: Long,
    val transactionId: String,
    val amount: BigDecimal,
)
