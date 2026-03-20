package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentJpaModel, Long> {
    fun findByTransactionKey(transactionKey: String): PaymentJpaModel?

    fun findAllByOrderId(orderId: Long): List<PaymentJpaModel>

    fun findAllByStatusAndRequestedAtBefore(status: PaymentStatus, before: ZonedDateTime): List<PaymentJpaModel>
}
