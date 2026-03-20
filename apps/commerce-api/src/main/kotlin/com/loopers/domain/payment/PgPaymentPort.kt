package com.loopers.domain.payment

interface PgPaymentPort {
    fun requestPayment(request: PgPaymentRequest): PgPaymentResponse
    fun queryPaymentStatus(transactionKey: String, userId: Long): PgPaymentStatusResponse
    fun isAvailable(): Boolean
}
