package com.loopers.domain.payment

data class PgTransactionDetail(
    val transactionKey: String,
    val orderId: Long,
    val status: PgResultStatus,
    val reason: String? = null,
)
