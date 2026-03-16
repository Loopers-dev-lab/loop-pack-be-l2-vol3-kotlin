package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HandlePaymentCallbackUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun execute(command: PaymentCommand.HandleCallback) {
        val payment = paymentRepository.findByOrderId(command.orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")

        val order = orderRepository.findById(OrderId(command.orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        val isProcessable = payment.status == PaymentStatus.REQUESTED || payment.status == PaymentStatus.TIMEOUT
        if (!isProcessable) return

        if (command.success) {
            payment.markSuccess(command.transactionKey)
            paymentRepository.save(payment)
            order.markPaid()
            orderRepository.save(order)
        } else {
            payment.markFailed(command.reason ?: "PG 콜백 실패")
            paymentRepository.save(payment)
            order.markFailed()
            orderRepository.save(order)
        }
    }
}
