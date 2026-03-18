package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Component

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(payment)
    }

    override fun findById(paymentId: Long): Payment? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(paymentId)
    }

    override fun findByOrderId(orderId: String): List<Payment> {
        return paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun findByTransactionKey(transactionKey: String): Payment? {
        return paymentJpaRepository.findByTransactionKeyAndDeletedAtIsNull(transactionKey)
    }
}
