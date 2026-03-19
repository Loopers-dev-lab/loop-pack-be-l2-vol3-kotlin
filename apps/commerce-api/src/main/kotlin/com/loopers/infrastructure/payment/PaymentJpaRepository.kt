package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Payment?
    fun findByOrderIdAndDeletedAtIsNull(orderId: String): List<Payment>
    fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): Payment?
    fun findByStatusAndCreatedAtBeforeAndDeletedAtIsNull(status: PaymentStatus, before: ZonedDateTime): List<Payment>
}
