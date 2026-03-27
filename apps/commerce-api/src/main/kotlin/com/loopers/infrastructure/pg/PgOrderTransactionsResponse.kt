package com.loopers.infrastructure.pg

data class PgOrderTransactionsResponse(
    val orderId: String,
    val transactions: List<PgTransactionSummary>,
)

data class PgTransactionSummary(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)
