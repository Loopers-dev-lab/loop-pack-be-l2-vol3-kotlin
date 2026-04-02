package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.repository.OrderOutboxRepository
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
    private val orderItemRepository: OrderItemRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
) {
    @Transactional
    fun execute(command: PaymentCommand.HandleCallback) {
        val payment = paymentRepository.findByOrderIdForUpdate(OrderId(command.orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")

        val isProcessable = payment.status == PaymentStatus.REQUESTED || payment.status == PaymentStatus.TIMEOUT
        if (!isProcessable) return

        val order = orderRepository.findByIdForUpdate(OrderId(command.orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        // transactionKey가 이미 저장되어 있으면 콜백의 transactionKey와 일치하는지 검증
        if (payment.transactionKey != null && payment.transactionKey != command.transactionKey) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "콜백 transactionKey 불일치. orderId=${command.orderId}",
            )
        }

        if (command.success) {
            payment.markSuccess(command.transactionKey)
            paymentRepository.save(payment)
            order.markPaid()
            orderRepository.save(order)
            val orderItems = orderItemRepository.findAllByOrderId(OrderId(command.orderId))
            orderOutboxRepository.saveAll(
                orderItems.map { item ->
                    OrderOutbox(
                        eventType = OrderOutbox.OrderOutboxEventType.PAYMENT_COMPLETED,
                        orderId = OrderId(command.orderId),
                        userId = UserId(order.refUserId.value),
                        totalAmount = Money(payment.amount.toBigDecimal()),
                        productId = ProductId(item.refProductId.value),
                        quantity = item.quantity,
                    )
                },
            )
        } else {
            val reason = command.reason ?: "PG 콜백 실패"
            payment.markFailed(reason)
            paymentRepository.save(payment)
            order.markFailed()
            orderRepository.save(order)
            orderOutboxRepository.save(
                OrderOutbox(
                    eventType = OrderOutbox.OrderOutboxEventType.PAYMENT_FAILED,
                    orderId = OrderId(command.orderId),
                    userId = UserId(order.refUserId.value),
                    reason = reason,
                ),
            )
        }
    }
}
