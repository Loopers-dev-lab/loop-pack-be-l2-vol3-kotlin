package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByIdempotencyKey(idempotencyKey: PaymentIdempotencyKey): Payment?
    fun findActiveByOrderId(orderId: Long): Payment?
    fun findAllByOrderId(orderId: Long): List<Payment>
    fun findPendingOlderThan(threshold: ZonedDateTime): List<Payment>
}
