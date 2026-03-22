package com.loopers.infrastructure.payment.pg

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * PG 결제 상태 조회 응답 (COMPLETED, FAILED, PENDING, TIMEOUT, etc.)
 */
data class PgPaymentStatusResponse(
    @JsonProperty("transactionId")
    val transactionId: String,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("reason")
    val reason: String?,
)
