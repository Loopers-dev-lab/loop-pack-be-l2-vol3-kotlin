package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentIdempotencyKey
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val paymentMapper: PaymentMapper,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        val entity = if (payment.id != null) {
            val existing = paymentJpaRepository.findByIdAndDeletedAtIsNull(payment.id)
                ?: return paymentMapper.toDomain(paymentJpaRepository.saveAndFlush(paymentMapper.toEntity(payment)))
            existing.status = payment.status
            existing.transactionKey = payment.transactionKey
            existing.reasonCode = payment.reasonCode
            paymentJpaRepository.saveAndFlush(existing)
        } else {
            paymentJpaRepository.saveAndFlush(paymentMapper.toEntity(payment))
        }
        return paymentMapper.toDomain(entity)
    }

    override fun saveIfPending(payment: Payment): Boolean {
        return paymentJpaRepository.updateIfPending(
            id = payment.id!!,
            status = payment.status,
            transactionKey = payment.transactionKey,
            reasonCode = payment.reasonCode,
            updatedAt = ZonedDateTime.now(),
        ) > 0
    }

    override fun hardDelete(id: Long) {
        val entity = paymentJpaRepository.findByIdAndDeletedAtIsNull(id) ?: return
        paymentJpaRepository.delete(entity)
        paymentJpaRepository.flush()
    }

    override fun findById(id: Long): Payment? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(id)?.let { paymentMapper.toDomain(it) }
    }

    override fun findByIdempotencyKey(idempotencyKey: PaymentIdempotencyKey): Payment? {
        return paymentJpaRepository.findByIdempotencyKeyAndDeletedAtIsNull(idempotencyKey.value)
            ?.let { paymentMapper.toDomain(it) }
    }

    override fun findByIdempotencyKeyForUpdate(idempotencyKey: PaymentIdempotencyKey): Payment? {
        return paymentJpaRepository.findByIdempotencyKeyForUpdate(idempotencyKey.value)
            ?.let { paymentMapper.toDomain(it) }
    }

    override fun findActiveByOrderId(orderId: Long): Payment? {
        return paymentJpaRepository.findByOrderIdAndStatusAndDeletedAtIsNull(orderId, Payment.Status.PENDING)
            ?.let { paymentMapper.toDomain(it) }
    }

    override fun findActiveByOrderIdForUpdate(orderId: Long): Payment? {
        return paymentJpaRepository.findByOrderIdAndStatusForUpdate(orderId, Payment.Status.PENDING)
            ?.let { paymentMapper.toDomain(it) }
    }

    override fun findAllByOrderId(orderId: Long): List<Payment> {
        return paymentJpaRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
            .map { paymentMapper.toDomain(it) }
    }

    override fun findPendingOlderThan(threshold: ZonedDateTime): List<Payment> {
        return paymentJpaRepository.findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
            Payment.Status.PENDING,
            threshold,
        ).map { paymentMapper.toDomain(it) }
    }
}
