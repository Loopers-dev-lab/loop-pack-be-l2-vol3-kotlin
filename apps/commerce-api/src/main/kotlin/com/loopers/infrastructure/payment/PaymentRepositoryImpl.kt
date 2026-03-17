package com.loopers.infrastructure.payment

import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import jakarta.persistence.LockModeType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findByOrderId(orderId: Long): PaymentEntity?
    fun findByStatusIn(statuses: List<PaymentStatus>, pageable: Pageable): List<PaymentEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockById(id: Long): PaymentEntity?

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

    override fun findByIdForUpdate(id: Long): Payment? {
        return paymentJpaRepository.findWithLockById(id)?.toDomain()
    }

    override fun findByOrderIdForUpdate(orderId: Long): Payment? {
        return paymentJpaRepository.findWithLockByOrderId(orderId)?.toDomain()
    }

    override fun findByStatusIn(statuses: List<PaymentStatus>, limit: Int): List<Payment> {
        return paymentJpaRepository.findByStatusIn(statuses, PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "id"))).map {
            it.toDomain()
        }
    }
}
