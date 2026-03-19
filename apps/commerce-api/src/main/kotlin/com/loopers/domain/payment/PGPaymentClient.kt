package com.loopers.domain.payment

/**
 * PG 시스템과의 결제 연동 인터페이스.
 * Domain Layer에 위치하여 기술 구현에 의존하지 않는다.
 */
interface PGPaymentClient {
    fun requestPayment(request: PGPaymentRequest): PGPaymentResponse
    fun getPaymentByTransactionKey(userId: Long, transactionKey: String): PGTransactionDetail
    fun getPaymentsByOrderId(userId: Long, orderId: String): PGOrderPayments
}

data class PGPaymentRequest(
    val userId: Long,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
)

data class PGPaymentResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PGTransactionDetail(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)

data class PGOrderPayments(
    val orderId: String,
    val transactions: List<PGPaymentResponse>,
)
