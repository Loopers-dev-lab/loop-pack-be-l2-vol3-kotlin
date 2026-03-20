package com.loopers.domain.payment

interface PgClient {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResponse
    fun getPaymentDetail(transactionKey: String, userId: Long): PgPaymentDetailResponse
}

data class PgPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
    val userId: Long,
)

data class PgPaymentResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PgPaymentDetailResponse(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)
