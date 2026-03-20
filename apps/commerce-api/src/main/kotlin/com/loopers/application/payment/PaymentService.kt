package com.loopers.application.payment

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    fun createPayment(payment: PaymentModel): PaymentModel {
        return paymentRepository.save(payment)
    }

    fun getPayment(id: Long): PaymentModel {
        return paymentRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 결제입니다.")
    }

    fun getPaymentByTransactionKey(transactionKey: String): PaymentModel {
        return paymentRepository.findByTransactionKey(transactionKey)
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

    fun getPendingPaymentsBefore(before: ZonedDateTime): List<PaymentModel> {
        return paymentRepository.findAllByStatusAndRequestedAtBefore(PaymentStatus.PENDING, before)
    }

    fun getRequestedPaymentsBefore(before: ZonedDateTime): List<PaymentModel> {
        return paymentRepository.findAllByStatusAndRequestedAtBefore(PaymentStatus.REQUESTED, before)
    }
}
