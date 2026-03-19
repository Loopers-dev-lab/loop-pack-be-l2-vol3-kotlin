package com.loopers.domain.payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(paymentId: Long): Payment?
    fun findByOrderId(orderId: String): List<Payment>
    fun findByTransactionKey(transactionKey: String): Payment?
    fun findByStatusAndOlderThan(status: PaymentStatus, minutes: Long): List<Payment>
}
