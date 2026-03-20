package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun createPayment(orderId: Long, amount: Long, expiresAt: ZonedDateTime): PaymentModel {
        paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중인 주문입니다: $orderId")
        }

        val payment = PaymentModel(
            orderId = orderId,
            amount = amount,
            expiresAt = expiresAt,
        )
        return try {
            paymentRepository.save(payment)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중인 주문입니다: $orderId")
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): PaymentModel {
        return paymentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findByOrderId(orderId: Long): PaymentModel {
        return paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다: $orderId")
    }

    @Transactional
    fun markSucceeded(id: Long, externalTransactionId: String): PaymentModel {
        val payment = findById(id)
        payment.markSucceeded(externalTransactionId)
        return paymentRepository.save(payment)
    }

    @Transactional
    fun markFailed(id: Long, failureReason: String): PaymentModel {
        val payment = findById(id)
        payment.markFailed(failureReason)
        return paymentRepository.save(payment)
    }

    @Transactional
    fun markExpired(id: Long): PaymentModel {
        val payment = findById(id)
        payment.markExpired()
        return paymentRepository.save(payment)
    }

    @Transactional
    fun expirePendingPayments(now: ZonedDateTime): Int {
        val pendingPayments = paymentRepository.findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(PaymentStatus.PENDING, now)
        pendingPayments.forEach { payment ->
            payment.markExpired()
            paymentRepository.save(payment)
        }
        return pendingPayments.size
    }
}
