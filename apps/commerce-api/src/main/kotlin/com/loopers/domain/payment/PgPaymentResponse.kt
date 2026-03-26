package com.loopers.domain.payment

sealed interface PgPaymentResponse {
    data class Accepted(val transactionKey: String) : PgPaymentResponse
    data class ImmediateFailure(val reasonCode: PaymentReasonCode) : PgPaymentResponse
    data object Timeout : PgPaymentResponse
    data object CircuitOpen : PgPaymentResponse
}

data class PgPaymentStatusResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)
