package com.loopers.domain.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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

    fun getPaymentByTransactionIdForUpdate(transactionId: String): Payment =
        paymentRepository.findByTransactionIdForUpdate(transactionId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보가 존재하지 않습니다")

    fun getPaymentByOrderIdForUpdate(orderId: Long): Payment? =
        paymentRepository.findByOrderIdForUpdate(orderId)

    @Transactional
    fun save(payment: Payment): Payment =
        paymentRepository.save(payment)

    @Transactional
    fun createPayment(
        orderId: Long,
        transactionId: String,
        amount: BigDecimal,
        cardType: String = "",
        cardNo: String = "",
    ): Payment {
        val payment = Payment.create(orderId, transactionId, amount, cardType, cardNo)
        return paymentRepository.save(payment)
    }

    @Transactional
    fun handlePaymentCallback(command: PaymentCallbackCommand) {
        val payment = getPaymentByTransactionIdForUpdate(command.transactionId)

        // 멱등성: INITIATED 상태가 아니면 무시
        if (payment.status != PaymentStatus.INITIATED) {
            return
        }

        // PG 콜백 status에 따라 분기 처리
        when (command.status?.uppercase()) {
            "FAILED" -> payment.markAsFailed()
            "CANCELLED" -> payment.markAsCancelled()
            "COMPLETED" -> payment.markAsCompleted(command.amount)
            else -> throw CoreException(
                ErrorType.BAD_REQUEST,
                "알 수 없는 결제 상태: ${command.status}",
            )
        }

        save(payment)
    }
}
