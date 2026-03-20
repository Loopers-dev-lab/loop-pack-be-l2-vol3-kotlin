package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(payment)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): Payment? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByTransactionKeyAndDeletedAtIsNull(transactionKey: String): Payment? {
        return paymentJpaRepository.findByTransactionKeyAndDeletedAtIsNull(transactionKey)
    }

    override fun findAllByOrderIdAndDeletedAtIsNull(orderId: Long): List<Payment> {
        return paymentJpaRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun existsByOrderIdAndStatusAndDeletedAtIsNull(orderId: Long, status: PaymentStatus): Boolean {
        return paymentJpaRepository.existsByOrderIdAndStatusAndDeletedAtIsNull(orderId, status)
    }
}
