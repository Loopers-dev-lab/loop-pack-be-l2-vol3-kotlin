package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: PaymentModel): PaymentModel {
        return paymentJpaRepository.save(payment)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): PaymentModel? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByOrderIdAndDeletedAtIsNull(orderId: Long): PaymentModel? {
        return paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(
        status: PaymentStatus,
        expiresAt: ZonedDateTime,
    ): List<PaymentModel> {
        return paymentJpaRepository.findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(status, expiresAt)
    }
}
