package com.loopers.application.payment

data class PaymentCallbackInfo(
    val transactionKey: String,
    val orderId: String,
    val status: String,
    val reason: String?,
)
