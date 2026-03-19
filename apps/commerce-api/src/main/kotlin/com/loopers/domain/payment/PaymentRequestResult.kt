package com.loopers.domain.payment

data class PaymentRequestResult(
    val transactionKey: String,
    val orderId: String,
    val cardType: Any,
    val cardNo: String,
    val amount: Long,
    val status: Any,
    val reason: String?,
)
