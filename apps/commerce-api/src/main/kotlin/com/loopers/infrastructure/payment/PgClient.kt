package com.loopers.infrastructure.payment

interface PgClient {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResponse
}

data class PgPaymentRequest(
    val orderId: Long,
    val amount: Long,
)

data class PgPaymentResponse(
    val orderId: Long,
    val amount: Long,
    val transactionId: String,
    val status: PgPaymentStatus,
)

enum class PgPaymentStatus {
    APPROVED,
}
