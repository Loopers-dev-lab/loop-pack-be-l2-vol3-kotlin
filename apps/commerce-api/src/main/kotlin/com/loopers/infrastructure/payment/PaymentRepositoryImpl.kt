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

    override fun findById(id: Long): Payment? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByTransactionKey(transactionKey: String): Payment? {
        return paymentJpaRepository.findByTransactionKeyAndDeletedAtIsNull(transactionKey)
    }

    override fun findByOrderId(orderId: Long): List<Payment> {
        return paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun findByPaymentStatusIn(statuses: List<PaymentStatus>): List<Payment> {
        return paymentJpaRepository.findByPaymentStatusInAndDeletedAtIsNull(statuses)
    }
}
