package com.loopers.domain.payment.repository

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: Long): Payment?
    fun findByStatusIn(statuses: List<PaymentStatus>): List<Payment>
    fun updateStatusConditionally(id: Long, expectedStatuses: List<PaymentStatus>, newStatus: PaymentStatus): Boolean
}
