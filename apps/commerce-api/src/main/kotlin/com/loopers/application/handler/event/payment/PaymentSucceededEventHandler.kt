package com.loopers.application.handler.event.payment

import com.loopers.domain.common.event.PaymentSucceededEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentSucceededEventHandler(
    private val completeOrderCommandPublisher: CompleteOrderCommandPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentSucceededEvent) {
        try {
            completeOrderCommandPublisher.publish(event.orderId)
            log.info("결제 성공 커맨드 발행: orderId={}", event.orderId)
        } catch (e: Exception) {
            log.error("결제 성공 커맨드 발행 실패: orderId={}, error={}", event.orderId, e.message, e)
        }
    }
}

interface CompleteOrderCommandPublisher {
    fun publish(orderId: Long)
}
