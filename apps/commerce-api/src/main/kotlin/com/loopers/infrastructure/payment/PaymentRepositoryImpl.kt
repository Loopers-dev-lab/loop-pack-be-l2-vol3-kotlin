package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Component

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
}
