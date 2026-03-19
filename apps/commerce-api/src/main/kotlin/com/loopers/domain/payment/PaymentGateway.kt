package com.loopers.domain.payment

interface PaymentGateway {
    fun requestPayment(
        userId: String,
        orderId: String,
        cardType: String,
        cardNo: String,
        amount: Long,
        callbackUrl: String,
    ): PaymentGatewayResponse?

    fun getTransactionStatus(
        userId: String,
        transactionKey: String,
    ): PaymentGatewayTransactionDetail?

    fun getTransactionsByOrderId(
        userId: String,
        orderId: String,
    ): List<PaymentGatewayResponse>
}

data class PaymentGatewayResponse(
    val transactionKey: String,
    val status: String,
    val reason: String?,
)

data class PaymentGatewayTransactionDetail(
    val transactionKey: String,
    val orderId: String,
    val status: String,
    val reason: String?,
)
