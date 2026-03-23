package com.loopers.application.payment

data class RequestPaymentCriteria(
    val orderId: Long,
    val cardType: String,
    val cardNo: String,
)

data class PaymentCallbackCriteria(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)
