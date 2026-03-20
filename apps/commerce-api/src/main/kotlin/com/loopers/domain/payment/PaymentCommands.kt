package com.loopers.domain.payment

import java.math.BigDecimal

data class CreatePaymentCommand(
    val orderId: Long,
    val userId: Long,
    val amount: BigDecimal,
    val cardType: String,
    val cardNo: String,
)

data class CompletePaymentCommand(
    val transactionKey: String,
)

data class FailPaymentCommand(
    val transactionKey: String,
    val reason: String? = null,
)
