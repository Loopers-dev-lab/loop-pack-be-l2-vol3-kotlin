package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Payment?
    fun findByOrderIdAndDeletedAtIsNull(orderId: String): List<Payment>
    fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): Payment?
}
