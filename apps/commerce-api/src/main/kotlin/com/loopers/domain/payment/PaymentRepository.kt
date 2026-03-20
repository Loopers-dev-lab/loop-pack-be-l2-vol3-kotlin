package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: PaymentModel): PaymentModel
    fun findById(id: Long): PaymentModel?
    fun findByTransactionKey(transactionKey: String): PaymentModel?
    fun findByOrderIdAndStatus(orderId: Long, status: PaymentStatus): PaymentModel?
    fun findAllByOrderId(orderId: Long): List<PaymentModel>
    fun findAllByStatusAndCreatedAtBefore(status: PaymentStatus, createdAt: ZonedDateTime): List<PaymentModel>
}
