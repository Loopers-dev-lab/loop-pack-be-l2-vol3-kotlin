package com.loopers.domain.payment.repository

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: Long): Payment?
    fun findByIdForUpdate(id: Long): Payment?
    fun findByOrderIdForUpdate(orderId: Long): Payment?
    fun findByStatusIn(statuses: List<PaymentStatus>, limit: Int = 100): List<Payment>
}
