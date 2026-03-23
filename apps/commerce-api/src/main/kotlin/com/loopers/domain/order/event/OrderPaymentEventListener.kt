package com.loopers.domain.order.event

import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.domain.payment.event.PaymentRequestedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderPaymentEventListener(
    private val orderService: OrderService,
) {
    @TransactionalEventListener
    fun onPaymentRequested(event: PaymentRequestedEvent) {
        orderService.markOrderAsPaymentRequested(event.userId, event.orderId)
    }

    @TransactionalEventListener
    fun onPaymentCallbackProcessed(event: PaymentCallbackProcessedEvent) {
        when (event.status.uppercase()) {
            "COMPLETED" -> orderService.markOrderAsPaid(event.orderId)
            // FAILED, CANCELLED, TIMEOUT는 Order 상태 변경 불필요
            // (PAYMENT_REQUESTED 상태로 유지되며, 사용자가 재시도 가능)
        }
    }
}
