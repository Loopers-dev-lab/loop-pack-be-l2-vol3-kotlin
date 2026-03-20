package com.loopers.domain.payment

import java.math.BigDecimal

interface PaymentClient {
    /**
     * PG로 결제를 요청합니다.
     *
     * @param userId 사용자 ID
     * @param transactionId 거래 ID
     * @param orderId 주문 ID
     * @param amount 거래 금액
     * @param cardType 카드 유형
     * @param cardNo 카드 번호
     * @return 결제 요청 결과 (requestId, signature 등)
     */
    fun requestPayment(
        userId: Long,
        transactionId: String,
        orderId: Long,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
    ): PaymentRequestResult
}
