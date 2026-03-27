package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentModel?
    fun findByOrderIdAndDeletedAtIsNull(orderId: Long): PaymentModel?
    fun findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(status: PaymentStatus, expiresAt: ZonedDateTime): List<PaymentModel>
}
