package com.loopers.domain.payment

import java.math.BigDecimal

/**
 * PG 결제 상태 조회 결과
 *
 * @param transactionId 거래 ID
 * @param status 결제 상태 (COMPLETED, FAILED, PENDING, TIMEOUT 등)
 * @param amount 거래 금액
 * @param reason 실패 사유 (null이면 성공)
 */
data class PaymentStatusCheckResult(
    val transactionId: String,
    val status: String,
    val amount: BigDecimal,
    val reason: String?,
)
