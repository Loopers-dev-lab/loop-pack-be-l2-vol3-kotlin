package com.loopers.domain.payment

interface PgClient {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResult
    fun getTransactionByOrderId(orderId: Long): PgTransactionDetail?
}
