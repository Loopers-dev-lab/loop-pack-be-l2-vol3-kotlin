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
            "FAILED", "CANCELLED" -> {
                // 결제 실패/취소 시 Order를 PENDING으로 복구 (재시도 가능)
                orderService.restoreOrderToPending(event.orderId)
            }
            // TIMEOUT은 배치 복구 서비스에서 별도 처리
        }
    }
}
