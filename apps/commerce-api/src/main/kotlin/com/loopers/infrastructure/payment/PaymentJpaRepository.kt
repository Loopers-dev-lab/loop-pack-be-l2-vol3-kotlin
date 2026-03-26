package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): PaymentModel?
    fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): PaymentModel?
    fun findByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: PaymentStatus): PaymentModel?
    fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<PaymentModel>
    fun findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
        status: PaymentStatus,
        createdAt: ZonedDateTime,
    ): List<PaymentModel>
}
