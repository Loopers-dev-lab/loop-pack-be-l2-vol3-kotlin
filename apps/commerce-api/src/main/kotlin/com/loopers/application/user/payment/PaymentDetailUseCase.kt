package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Service
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@Service
class PaymentDetailUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val paymentReadRepairService: PaymentReadRepairService,
) {

    fun detail(paymentId: Long, userId: Long): PaymentResult.Detail {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CoreException(ErrorType.PAYMENT_NOT_FOUND)
        }

        val reconciledPayment = if (!payment.isTerminal && payment.transactionKey != null) {
            paymentReadRepairService.repair(payment)
        } else {
            payment
        }

        val order = orderRepository.findById(reconciledPayment.orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

        return PaymentResult.Detail.from(reconciledPayment, order.status)
    }
}
