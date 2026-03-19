package com.loopers.domain.payment

data class PgPaymentResult(
    val transactionKey: String?,
    val status: PgResultStatus,
    val reason: String? = null,
) {
    init {
        if (status == PgResultStatus.SUCCESS) {
            require(!transactionKey.isNullOrBlank()) {
                "SUCCESS 상태에서는 transactionKey가 필수입니다."
            }
        }
    }
}

enum class PgResultStatus {
    SUCCESS,
    FAILED,
    TIMEOUT,
}
