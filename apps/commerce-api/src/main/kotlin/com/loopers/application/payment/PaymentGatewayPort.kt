package com.loopers.application.payment

interface PaymentGatewayPort {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResponse
    fun getTransactionStatus(userId: String, transactionKey: String): PgTransactionDetail
    fun getTransactionsByOrderId(userId: String, orderId: String): List<PgTransactionDetail>
}

data class PgPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
    val userId: String,
)

data class PgPaymentResponse(
    val success: Boolean,
    val transactionKey: String?,
    val status: String?,
    val reason: String?,
)

data class PgTransactionDetail(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)
