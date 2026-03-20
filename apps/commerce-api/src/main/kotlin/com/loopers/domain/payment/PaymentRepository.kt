package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: PaymentModel): PaymentModel

    fun findById(id: Long): PaymentModel?

    fun findByTransactionKey(transactionKey: String): PaymentModel?

    fun findByOrderId(orderId: Long): List<PaymentModel>

    fun findAllByStatusAndRequestedAtBefore(status: PaymentStatus, before: ZonedDateTime): List<PaymentModel>
}
