package com.loopers.infrastructure.payment

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByOrderId(orderId: Long): PaymentEntity?
    fun findByStatusIn(statuses: List<PaymentStatus>): List<PaymentEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByOrderId(orderId: Long): PaymentEntity?
}

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(PaymentEntity.fromDomain(payment)).toDomain()
    }

    override fun findById(id: Long): Payment? {
        return paymentJpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByOrderId(orderId: Long): Payment? {
        return paymentJpaRepository.findByOrderId(orderId)?.toDomain()
    }

    override fun findByOrderIdForUpdate(orderId: Long): Payment? {
        return paymentJpaRepository.findWithLockByOrderId(orderId)?.toDomain()
    }

    override fun findByStatusIn(statuses: List<PaymentStatus>): List<Payment> {
        return paymentJpaRepository.findByStatusIn(statuses).map { it.toDomain() }
    }

    override fun updateStatusConditionally(
        id: Long,
        expectedStatuses: List<PaymentStatus>,
        newStatus: PaymentStatus,
    ): Boolean {
        val entity = paymentJpaRepository.findById(id).orElse(null) ?: return false
        if (entity.status !in expectedStatuses) return false
        entity.status = newStatus
        paymentJpaRepository.save(entity)
        return true
    }
}
