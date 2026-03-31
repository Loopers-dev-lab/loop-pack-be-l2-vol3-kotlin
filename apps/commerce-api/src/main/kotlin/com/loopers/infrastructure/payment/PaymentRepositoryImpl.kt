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
        if (payment.id == 0L) {
            return paymentJpaRepository.save(PaymentJpaModel.from(payment)).toModel()
        }
        val existing = paymentJpaRepository.findById(payment.id).orElseThrow()
        existing.updateFrom(payment)
        return existing.toModel()
    }

    override fun findById(id: Long): PaymentModel? {
        return paymentJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findByTransactionKey(transactionKey: String): PaymentModel? {
        return paymentJpaRepository.findByTransactionKey(transactionKey)?.toModel()
    }

    override fun findByTransactionKeyWithLock(transactionKey: String): PaymentModel? {
        return paymentJpaRepository.findByTransactionKeyWithLock(transactionKey)?.toModel()
    }

    override fun findByIdWithLock(id: Long): PaymentModel? {
        return paymentJpaRepository.findByIdWithLock(id)?.toModel()
    }

    override fun findByOrderId(orderId: Long): List<PaymentModel> {
        return paymentJpaRepository.findAllByOrderId(orderId).map { it.toModel() }
    }

    override fun findAllByStatusAndRequestedAtBefore(
        status: PaymentStatus,
        before: ZonedDateTime,
    ): List<PaymentModel> {
        return paymentJpaRepository.findAllByStatusAndRequestedAtBefore(status, before)
            .map { it.toModel() }
    }
}
