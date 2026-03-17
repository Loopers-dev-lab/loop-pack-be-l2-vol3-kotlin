package com.loopers.domain.payment

interface PaymentRepository {
    fun findByTransactionId(transactionId: String): Payment?

    fun findByOrderId(orderId: Long): Payment?

    fun findByTransactionIdForUpdate(transactionId: String): Payment?

    fun findByOrderIdForUpdate(orderId: Long): Payment?

    fun save(payment: Payment): Payment
}
