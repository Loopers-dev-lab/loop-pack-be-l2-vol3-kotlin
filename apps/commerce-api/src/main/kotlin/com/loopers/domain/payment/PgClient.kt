package com.loopers.domain.payment

interface PgClient {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResponse
    fun getPaymentStatus(userId: Long, transactionKey: String): PgPaymentStatusResponse
}

data class PgPaymentRequest(
    val orderId: String,
    val userId: Long,
    val amount: Long,
    val callbackUrl: String,
    val cardType: String,
    val cardNo: String,
)

data class PgPaymentResponse(
    val transactionKey: String,
    val status: String,
)

data class PgPaymentStatusResponse(
    val transactionKey: String,
    val status: String,
    val reason: String? = null,
)
