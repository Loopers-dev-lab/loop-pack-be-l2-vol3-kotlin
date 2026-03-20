package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<PaymentModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentModel?
    fun findByOrderIdAndDeletedAtIsNull(orderId: Long): PaymentModel?
}
