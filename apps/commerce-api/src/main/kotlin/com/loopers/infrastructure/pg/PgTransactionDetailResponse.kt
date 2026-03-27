package com.loopers.infrastructure.pg

data class PgTransactionDetailResponse(
    val transactionKey: String,
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val status: String,
    val reason: String?,
) {
    fun isTerminal(): Boolean = status == "SUCCESS" || status == "FAILED"
    fun isSuccess(): Boolean = status == "SUCCESS"
}
