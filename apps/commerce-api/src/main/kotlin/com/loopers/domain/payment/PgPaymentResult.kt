package com.loopers.domain.payment

data class PgPaymentResult(
    val transactionKey: String?,
    val status: PgResultStatus,
    val reason: String? = null,
)

enum class PgResultStatus {
    SUCCESS,
    FAILED,
    TIMEOUT,
}
