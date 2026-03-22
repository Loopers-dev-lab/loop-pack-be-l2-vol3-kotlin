package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCompleted(event: PaymentEvent.Completed) {
        log.info("결제 완료 이벤트 수신: orderId={}, userId={}, totalAmount={}", event.orderId, event.userId, event.totalAmount)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFailed(event: PaymentEvent.Failed) {
        log.info("결제 실패 이벤트 수신: orderId={}, userId={}, reason={}", event.orderId, event.userId, event.reason)
    }
}
