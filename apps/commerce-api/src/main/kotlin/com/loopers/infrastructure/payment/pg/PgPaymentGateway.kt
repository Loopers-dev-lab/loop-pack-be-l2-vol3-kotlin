package com.loopers.infrastructure.payment.pg

import java.math.BigDecimal

interface PgPaymentGateway {
    /**
     * PG로 결제를 요청합니다.
     *
     * @param userId 사용자 ID
     * @param transactionId 거래 ID
     * @param orderId 주문 ID
     * @param amount 거래 금액
     * @param cardType 카드 유형
     * @param cardNo 카드 번호
     * @param callbackUrl 콜백 URL
     * @return 결제 요청 결과 (requestId, signature 등)
     */
    fun requestPayment(
        userId: Long,
        transactionId: String,
        orderId: Long,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
        callbackUrl: String,
    ): PaymentRequestResult

    /**
     * PG에서 보낸 서명을 검증합니다.
     *
     * @param transactionId 거래 ID
     * @param amount 거래 금액
     * @param signature PG에서 보낸 서명
     * @return 서명이 유효하면 true, 아니면 false
     */
    fun verifySignature(transactionId: String, amount: BigDecimal, signature: String): Boolean

    /**
     * PG 결제 요청 결과
     */
    data class PaymentRequestResult(
        val requestId: String,
        val transactionId: String,
        val status: String,
        val signature: String,
    )
}
