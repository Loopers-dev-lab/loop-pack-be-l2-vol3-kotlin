package com.loopers.domain.payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findByIdAndDeletedAtIsNull(id: Long): Payment?
    fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): Payment?
    fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<Payment>
    fun existsByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: PaymentStatus): Boolean
}
