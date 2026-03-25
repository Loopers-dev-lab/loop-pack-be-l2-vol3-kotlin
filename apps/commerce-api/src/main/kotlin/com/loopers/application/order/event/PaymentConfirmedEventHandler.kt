package com.loopers.application.order.event

import com.loopers.application.payment.event.PaymentConfirmedEvent
import com.loopers.application.payment.event.PaymentFailedEvent
import com.loopers.domain.order.OrderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentConfirmedEventHandler(
    private val orderService: OrderService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handlePaymentConfirmed(event: PaymentConfirmedEvent) {
        log.info("[Event] PaymentConfirmed: orderId=${event.orderId}, paymentId=${event.paymentId}")

        try {
            orderService.updateStatus(event.orderId) { order ->
                order.pay(event.paidAt)
            }
        } catch (ex: Exception) {
            log.error("[Event] 주문 상태 변경 실패: orderId=${event.orderId}, error=${ex.message}", ex)
        }

        log.info("[UserAction] PAYMENT_CONFIRMED: orderId=${event.orderId}, amount=${event.amount}")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        log.info("[Event] PaymentFailed: orderId=${event.orderId}, reason=${event.reason}")
        log.info("[UserAction] PAYMENT_FAILED: orderId=${event.orderId}, reason=${event.reason}")
    }
}
