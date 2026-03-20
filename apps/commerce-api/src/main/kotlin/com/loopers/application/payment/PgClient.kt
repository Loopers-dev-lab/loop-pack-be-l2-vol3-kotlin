package com.loopers.application.payment

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

data class PgTransactionInfo(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)

data class PgOrderPaymentsResponse(
    val orderId: String,
    val transactions: List<PgPaymentResponse>,
)

interface PgClient {
    fun requestPayment(memberId: Long, request: PgPaymentRequest): PgPaymentResponse

    fun getPaymentStatus(memberId: Long, transactionKey: String): PgTransactionInfo

    fun getPaymentsByOrderId(memberId: Long, orderId: String): PgOrderPaymentsResponse
}
