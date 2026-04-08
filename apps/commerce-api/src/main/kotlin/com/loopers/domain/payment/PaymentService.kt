package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun pay(command: CreatePaymentCommand): PaymentInfo {
        val existingSuccess = paymentRepository.findByOrderIdAndStatus(command.orderId, PaymentStatus.SUCCESS)
        if (existingSuccess != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 결제가 완료된 주문입니다.")
        }

        val existingPending = paymentRepository.findByOrderIdAndStatus(command.orderId, PaymentStatus.PENDING)
        existingPending?.markFailed("새 결제 요청으로 인한 기존 PENDING 취소")

        val payment = PaymentModel(
            orderId = command.orderId,
            userId = command.userId,
            amount = command.amount,
            cardType = command.cardType,
            cardNo = command.cardNo,
        )
        val saved = paymentRepository.save(payment)
        return PaymentInfo.from(saved)
    }

    @Transactional
    fun complete(command: CompletePaymentCommand): PaymentInfo {
        val payment = paymentRepository.findByTransactionKey(command.transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        payment.markSuccess()
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun fail(command: FailPaymentCommand): PaymentInfo {
        val payment = paymentRepository.findByTransactionKey(command.transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        payment.markFailed(command.reason)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun failById(paymentId: Long, reason: String?): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        payment.markFailed(reason)
        return PaymentInfo.from(payment)
    }

    @Transactional
    fun updateTransactionKey(paymentId: Long, transactionKey: String): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        payment.updateTransactionKey(transactionKey)
        return PaymentInfo.from(payment)
    }

    @Transactional(readOnly = true)
    fun getPayment(paymentId: Long): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        return PaymentInfo.from(payment)
    }

    @Transactional(readOnly = true)
    fun getPaymentByTransactionKey(transactionKey: String): PaymentInfo {
        val payment = paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        return PaymentInfo.from(payment)
    }

    @Transactional(readOnly = true)
    fun getPendingPayments(before: ZonedDateTime): List<PaymentInfo> {
        return paymentRepository.findAllByStatusAndCreatedAtBefore(PaymentStatus.PENDING, before)
            .map { PaymentInfo.from(it) }
    }
}
