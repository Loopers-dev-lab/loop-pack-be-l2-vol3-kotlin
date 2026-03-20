package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    fun createPayment(orderId: Long, amount: Long, expiresAt: ZonedDateTime): PaymentModel {
        val payment = PaymentModel(
            orderId = orderId,
            amount = amount,
            expiresAt = expiresAt,
        )
        return paymentRepository.save(payment)
    }

    fun findById(id: Long): PaymentModel {
        return paymentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다: $id")
    }

    fun markSucceeded(id: Long, externalTransactionId: String): PaymentModel {
        val payment = findById(id)
        payment.markSucceeded(externalTransactionId)
        return paymentRepository.save(payment)
    }

    fun markFailed(id: Long, failureReason: String): PaymentModel {
        val payment = findById(id)
        payment.markFailed(failureReason)
        return paymentRepository.save(payment)
    }

    fun markExpired(id: Long): PaymentModel {
        val payment = findById(id)
        payment.markExpired()
        return paymentRepository.save(payment)
    }
}
