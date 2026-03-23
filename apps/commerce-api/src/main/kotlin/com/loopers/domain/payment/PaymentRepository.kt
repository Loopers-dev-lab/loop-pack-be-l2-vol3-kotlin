package com.loopers.domain.payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByTransactionKey(transactionKey: String): Payment?
    fun findByOrderId(orderId: Long): List<Payment>
    fun findByPaymentStatusIn(statuses: List<PaymentStatus>): List<Payment>
}
