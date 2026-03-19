package com.loopers.infrastructure.payment

/**
 * PG 시뮬레이터 API 응답 래퍼
 */
data class PgApiResponse<T>(
    val meta: Metadata,
    val data: T?,
) {
    data class Metadata(
        val result: String,
        val errorCode: String?,
        val message: String?,
    )
}

/**
 * PG 결제 요청 DTO
 */
data class PgPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
)

/**
 * PG 트랜잭션 응답 (결제 요청 시)
 */
data class PgTransactionResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

/**
 * PG 트랜잭션 상세 응답
 */
data class PgTransactionDetailResponse(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
)

/**
 * PG 주문 조회 응답
 */
data class PgOrderResponse(
    val orderId: String,
    val transactions: List<PgTransactionResponse>,
)
