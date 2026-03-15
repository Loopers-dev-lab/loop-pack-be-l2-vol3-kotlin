package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {

    fun getPaymentByTransactionId(transactionId: String): Payment =
        paymentRepository.findByTransactionId(transactionId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")

    fun getPaymentByOrderId(orderId: Long): Payment? =
        paymentRepository.findByOrderId(orderId)

    @Transactional
    fun save(payment: Payment): Payment =
        paymentRepository.save(payment)

    @Transactional
    fun createPayment(
        orderId: Long,
        transactionId: String,
        amount: java.math.BigDecimal,
        cardType: String = "",
        cardNo: String = "",
    ): Payment {
        val payment = Payment.create(orderId, transactionId, amount, cardType, cardNo)
        return paymentRepository.save(payment)
    }
}
