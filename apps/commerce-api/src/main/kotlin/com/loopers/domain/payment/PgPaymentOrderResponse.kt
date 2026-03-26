package com.loopers.domain.payment

data class PgPaymentOrderResponse(
    val orderId: String,
    val transactions: List<Transaction>,
) {
    data class Transaction(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )
}
