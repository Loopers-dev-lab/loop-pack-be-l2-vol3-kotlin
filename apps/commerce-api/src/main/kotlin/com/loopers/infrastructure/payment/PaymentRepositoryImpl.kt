package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    override fun findByTransactionId(transactionId: String): Payment? =
        paymentJpaRepository.findByTransactionId(transactionId)

    override fun findByOrderId(orderId: Long): Payment? =
        paymentJpaRepository.findByOrderId(orderId)

    override fun findByTransactionIdForUpdate(transactionId: String): Payment? =
        paymentJpaRepository.findByTransactionIdForUpdate(transactionId)

    override fun findByOrderIdForUpdate(orderId: Long): Payment? =
        paymentJpaRepository.findByOrderIdForUpdate(orderId)

    override fun save(payment: Payment): Payment =
        paymentJpaRepository.save(payment)
}
