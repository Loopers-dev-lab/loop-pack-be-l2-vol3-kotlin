package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    @Transactional
    fun createPayment(userId: Long, orderId: Long, amount: BigDecimal, cardType: String, cardNo: String): Payment {
        val maskedCardNo = Payment.maskCardNo(cardNo)
        val payment = Payment(orderId = orderId, userId = userId, amount = amount, cardType = cardType, cardNo = maskedCardNo)
        return paymentRepository.save(payment)
    }

    @Transactional(readOnly = true)
    fun getPaymentByOrderId(orderId: Long): Payment? {
        return paymentRepository.findByOrderId(orderId)
    }

    @Transactional(readOnly = true)
    fun getPaymentByTransactionKey(transactionKey: String): Payment? {
        return paymentRepository.findByTransactionKey(transactionKey)
    }

    @Transactional(readOnly = true)
    fun getPayment(paymentId: Long): Payment {
        return paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
    }
}
