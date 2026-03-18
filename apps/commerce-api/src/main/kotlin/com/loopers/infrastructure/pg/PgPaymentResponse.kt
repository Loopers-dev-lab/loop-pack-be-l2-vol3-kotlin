package com.loopers.infrastructure.pg

data class PgPaymentResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)
