package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun createPayment(payment: Payment): Payment {
        return paymentRepository.save(payment)
    }

    @Transactional(readOnly = true)
    fun getPayment(id: Long): Payment {
        return paymentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getPaymentByTransactionKey(transactionKey: String): Payment {
        return paymentRepository.findByTransactionKeyAndDeletedAtIsNull(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getPaymentsByOrderId(orderId: Long): List<Payment> {
        return paymentRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
    }

    @Transactional(readOnly = true)
    fun hasSuccessfulPayment(orderId: Long): Boolean {
        return paymentRepository.existsByOrderIdAndStatusAndDeletedAtIsNull(orderId, PaymentStatus.SUCCESS)
    }

    @Transactional
    fun updateAfterPgResponse(paymentId: Long, transactionKey: String?, status: String, reason: String?) {
        val payment = getPayment(paymentId)
        if (transactionKey != null) {
            payment.assignTransactionKey(transactionKey)
        }
        if (status == "FAILED") {
            payment.complete(PaymentStatus.FAILED, reason)
        }
    }
}
