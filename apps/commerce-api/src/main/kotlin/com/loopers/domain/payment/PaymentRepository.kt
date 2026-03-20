package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: PaymentModel): PaymentModel
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentModel?
    fun findByOrderIdAndDeletedAtIsNull(orderId: Long): PaymentModel?
    fun findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(status: PaymentStatus, expiresAt: ZonedDateTime): List<PaymentModel>
}
