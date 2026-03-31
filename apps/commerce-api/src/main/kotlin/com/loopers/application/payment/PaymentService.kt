package com.loopers.application.payment

import com.loopers.domain.common.event.PaymentFailedEvent
import com.loopers.domain.common.event.PaymentRequestedEvent
import com.loopers.domain.common.event.PaymentSucceededEvent
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun createPayment(payment: PaymentModel): PaymentModel {
        val saved = paymentRepository.save(payment)
        eventPublisher.publishEvent(
            PaymentRequestedEvent(
                paymentId = saved.id,
                orderId = saved.orderId,
                memberId = saved.memberId,
                amount = saved.amount,
            ),
        )
        return saved
    }

    fun getPayment(id: Long): PaymentModel {
        return paymentRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    fun getPaymentByTransactionKey(transactionKey: String): PaymentModel {
        return paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    fun getPaymentByTransactionKeyWithLock(transactionKey: String): PaymentModel {
        return paymentRepository.findByTransactionKeyWithLock(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    fun getPaymentWithLock(id: Long): PaymentModel {
        return paymentRepository.findByIdWithLock(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    fun getPaymentsByOrderId(orderId: Long): List<PaymentModel> {
        return paymentRepository.findByOrderId(orderId)
    }

    fun hasActivePayment(orderId: Long): Boolean {
        return paymentRepository.findByOrderId(orderId)
            .any { it.isActive() }
    }

    fun savePayment(payment: PaymentModel): PaymentModel {
        return paymentRepository.save(payment)
    }

    fun completePayment(payment: PaymentModel): PaymentModel {
        val completed = payment.markSuccess()
        val saved = paymentRepository.save(completed)
        eventPublisher.publishEvent(
            PaymentSucceededEvent(
                paymentId = saved.id,
                orderId = saved.orderId,
                memberId = saved.memberId,
                transactionKey = saved.transactionKey!!,
            ),
        )
        return saved
    }

    fun failPayment(payment: PaymentModel, reason: String?): PaymentModel {
        val failed = payment.markFailed(reason)
        val saved = paymentRepository.save(failed)
        eventPublisher.publishEvent(
            PaymentFailedEvent(
                paymentId = saved.id,
                orderId = saved.orderId,
                memberId = saved.memberId,
                failReason = reason,
            ),
        )
        return saved
    }

    fun getPendingPaymentsBefore(before: ZonedDateTime): List<PaymentModel> {
        return paymentRepository.findAllByStatusAndRequestedAtBefore(PaymentStatus.PENDING, before)
    }

    fun getRequestedPaymentsBefore(before: ZonedDateTime): List<PaymentModel> {
        return paymentRepository.findAllByStatusAndRequestedAtBefore(PaymentStatus.REQUESTED, before)
    }
}
