package com.loopers.application.api.payment.dto

import java.math.BigDecimal

data class PaymentCallbackCommand(
    val transactionId: String,
    val orderId: Long,
    val amount: BigDecimal,
    val status: String? = null,
    val reason: String? = null,
)
