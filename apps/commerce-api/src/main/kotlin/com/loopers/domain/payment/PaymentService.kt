package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    fun createPayment(
        userId: Long,
        orderId: String,
        cardType: CardType,
        cardNo: String,
        amount: Long,
    ): Payment {
        val payment = Payment(
            userId = userId,
            orderId = orderId,
            cardType = cardType,
            cardNo = cardNo,
            amount = amount,
        )
        return paymentRepository.save(payment)
    }

    fun getPayment(paymentId: Long): Payment {
        return paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다.")
    }

    fun getPaymentsByOrderId(orderId: String): List<Payment> {
        return paymentRepository.findByOrderId(orderId)
    }

    fun getPaymentByTransactionKey(transactionKey: String): Payment {
        return paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "해당 거래를 찾을 수 없습니다.")
    }

    fun markPending(paymentId: Long, transactionKey: String) {
        val payment = getPayment(paymentId)
        payment.markPending(transactionKey)
        paymentRepository.save(payment)
    }

    fun markSuccess(paymentId: Long) {
        val payment = getPayment(paymentId)
        payment.markSuccess()
        paymentRepository.save(payment)
    }

    fun markFailed(paymentId: Long, reason: String) {
        val payment = getPayment(paymentId)
        payment.markFailed(reason)
        paymentRepository.save(payment)
    }
}
