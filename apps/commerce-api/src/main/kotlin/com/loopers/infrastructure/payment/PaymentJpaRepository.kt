package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByTransactionKey(transactionKey: String): PaymentEntity?
    fun findByOrderId(orderId: Long): PaymentEntity?
    fun findAllByStatusAndCreatedAtBefore(status: PaymentStatus, createdAt: ZonedDateTime): List<PaymentEntity>
}
