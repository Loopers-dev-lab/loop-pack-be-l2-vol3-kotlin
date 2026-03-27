package com.loopers.application.handler.event.payment

import com.loopers.domain.common.event.PaymentFailedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentFailedEventHandler(
    private val paymentCompensationPublisher: PaymentCompensationPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PaymentFailedEvent) {
        try {
            paymentCompensationPublisher.publish(event.orderId, event.paymentId)
            log.info("결제 실패 보상 커맨드 발행: orderId={}, paymentId={}", event.orderId, event.paymentId)
        } catch (e: Exception) {
            log.error("결제 실패 보상 커맨드 발행 실패: orderId={}, error={}", event.orderId, e.message, e)
        }
    }
}

interface PaymentCompensationPublisher {
    fun publish(orderId: Long, paymentId: Long)
}
