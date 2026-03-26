package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Component

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val paymentMapper: PaymentMapper,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        val entity = if (payment.id == null) {
            paymentMapper.toEntity(payment)
        } else {
            val existing = paymentJpaRepository.getReferenceById(payment.id)
            paymentMapper.update(existing, payment)
            existing
        }

        return paymentMapper.toDomain(paymentJpaRepository.save(entity))
    }

    override fun findById(id: Long): Payment? = paymentJpaRepository.findById(id)
        .map { paymentMapper.toDomain(it) }
        .orElse(null)

    override fun findLatestByOrderId(orderId: Long): Payment? =
        paymentJpaRepository.findTopByOrderIdOrderByIdDesc(orderId)?.let(paymentMapper::toDomain)

    override fun findLatestByOrderId(orderId: Long, memberId: Long): Payment? =
        paymentJpaRepository.findTopByOrderIdAndMemberIdOrderByIdDesc(orderId, memberId)?.let(paymentMapper::toDomain)

    override fun findByPgTransactionKey(transactionKey: String): Payment? =
        paymentJpaRepository.findByPgTransactionKey(transactionKey)?.let(paymentMapper::toDomain)
}
