package com.loopers.infrastructure.payment.pg

data class PgPaymentResponse(
    val transactionKey: String,
    val orderId: String,
    val cardType: Any,
    val cardNo: String,
    val amount: Long,
    val status: Any,
    val reason: String?,
)
