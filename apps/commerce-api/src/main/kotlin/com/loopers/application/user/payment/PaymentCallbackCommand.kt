package com.loopers.application.user.payment

class PaymentCallbackCommand(
    val paymentId: Long,
    val transactionKey: String,
    val status: String,
    val reason: String?,
)
