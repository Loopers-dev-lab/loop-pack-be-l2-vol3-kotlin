package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: PaymentModel): PaymentModel {
        return paymentJpaRepository.save(payment)
    }

    override fun findById(id: Long): PaymentModel? {
        return paymentJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByTransactionKey(transactionKey: String): PaymentModel? {
        return paymentJpaRepository.findByTransactionKeyAndDeletedAtIsNull(transactionKey)
    }

    override fun findByOrderIdAndStatus(orderId: Long, status: PaymentStatus): PaymentModel? {
        return paymentJpaRepository.findByOrderIdAndStatusAndDeletedAtIsNull(orderId, status)
    }

    override fun findAllByOrderId(orderId: Long): List<PaymentModel> {
        return paymentJpaRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun findAllByStatusAndCreatedAtBefore(status: PaymentStatus, createdAt: ZonedDateTime): List<PaymentModel> {
        return paymentJpaRepository.findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(status, createdAt)
    }
}
