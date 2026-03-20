package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Payment?
    fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): Payment?
    fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<Payment>
    fun existsByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: PaymentStatus): Boolean
}
