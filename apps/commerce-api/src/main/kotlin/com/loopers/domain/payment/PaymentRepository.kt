package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: Payment): Long
    fun findById(id: Long): Payment?
    fun findByTransactionKey(transactionKey: String): Payment?
    fun findByOrderId(orderId: Long): Payment?
    fun findAllByStatusAndCreatedBefore(status: PaymentStatus, before: ZonedDateTime): List<Payment>
}
