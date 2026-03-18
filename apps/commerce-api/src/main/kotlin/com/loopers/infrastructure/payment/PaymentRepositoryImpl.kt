package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class PaymentRepositoryImpl(
    private val jpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    override fun save(payment: Payment): Long {
        val entity = PaymentMapper.toEntity(payment)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "Payment 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): Payment? {
        return jpaRepository.findById(id).orElse(null)?.let { PaymentMapper.toDomain(it) }
    }

    override fun findByTransactionKey(transactionKey: String): Payment? {
        return jpaRepository.findByTransactionKey(transactionKey)?.let { PaymentMapper.toDomain(it) }
    }

    override fun findByOrderId(orderId: Long): Payment? {
        return jpaRepository.findByOrderId(orderId)?.let { PaymentMapper.toDomain(it) }
    }

    override fun findAllByStatusAndCreatedBefore(status: PaymentStatus, before: ZonedDateTime): List<Payment> {
        return jpaRepository.findAllByStatusAndCreatedAtBefore(status, before)
            .map { PaymentMapper.toDomain(it) }
    }
}
