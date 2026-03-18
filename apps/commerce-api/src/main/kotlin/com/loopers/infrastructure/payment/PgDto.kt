package com.loopers.infrastructure.payment

data class PgPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
)

data class PgPaymentResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PgTransactionDetailResponse(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)

data class PgOrderResponse(
    val orderId: String,
    val transactions: List<PgPaymentResponse>,
)
