package com.loopers.application.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentError
import com.loopers.domain.payment.PaymentException
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

data class PendingPaymentResult(val paymentId: Long, val amount: Long)

@Component
class PaymentTransactionService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun createPendingPayment(userId: Long, command: RequestPaymentCommand): PendingPaymentResult {
        val order = orderRepository.findById(command.orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: ${command.orderId}")

        order.assertOwnedBy(userId)

        if (order.status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 가능한 주문 상태가 아닙니다. 현재 상태: ${order.status}")
        }

        val existingPayment = paymentRepository.findByOrderId(command.orderId)
        if (existingPayment != null && existingPayment.status != PaymentStatus.FAILED) {
            throw PaymentException(PaymentError.ALREADY_PAID, "이미 진행 중이거나 완료된 결제가 있습니다. 주문: ${command.orderId}")
        }

        val cardType = try {
            CardType.valueOf(command.cardType)
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 카드 타입입니다: ${command.cardType}")
        }

        val payment = Payment.create(
            refOrderId = command.orderId,
            refUserId = userId,
            cardType = cardType,
            cardNo = command.cardNo,
            amount = order.totalAmount.amount,
        )

        val paymentId = paymentRepository.save(payment)
        return PendingPaymentResult(paymentId, order.totalAmount.amount)
    }

    @Transactional
    fun markRequested(paymentId: Long, transactionKey: String) {
        val payment = paymentRepository.findById(paymentId) ?: return
        if (payment.isTerminal()) return
        val updated = payment.markRequested(transactionKey)
        paymentRepository.save(updated)
    }

    @Transactional
    fun markFailed(paymentId: Long, reason: String) {
        val payment = paymentRepository.findById(paymentId) ?: return
        if (payment.isTerminal()) return
        val updated = payment.fail(reason)
        paymentRepository.save(updated)
    }
}
