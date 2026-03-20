package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentEntity?
    fun findByIdempotencyKeyAndDeletedAtIsNull(idempotencyKey: String): PaymentEntity?
    fun findByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: Payment.Status): PaymentEntity?
    fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<PaymentEntity>
    fun findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
        status: Payment.Status,
        createdAt: ZonedDateTime,
    ): List<PaymentEntity>
}
