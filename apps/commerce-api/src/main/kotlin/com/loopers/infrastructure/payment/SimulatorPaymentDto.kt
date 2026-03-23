package com.loopers.infrastructure.payment

/**
 * PG Simulator 전용 요청/응답 DTO.
 * 특정 PG사 API 스펙에 맞춘 변환용이며, 도메인 DTO(PGPaymentRequest/Response 등)와 분리한다.
 * 다른 PG사 연동 시 별도 DTO(예: TossPaymentDto)를 만들어 구현체별로 관리한다.
 */
class SimulatorPaymentDto {

    /** PG Simulator 공통 응답 래퍼 */
    data class ApiResponse<T>(
        val meta: Meta,
        val data: T,
    )

    data class Meta(
        val result: String,
    )

    data class PaymentRequestBody(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val callbackUrl: String,
    )

    data class TransactionResponseBody(
        val transactionKey: String,
        val status: String,
        val reason: String?,
    )

    data class TransactionDetailResponseBody(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )

    data class OrderResponseBody(
        val orderId: String,
        val transactions: List<TransactionResponseBody>,
    )
}
