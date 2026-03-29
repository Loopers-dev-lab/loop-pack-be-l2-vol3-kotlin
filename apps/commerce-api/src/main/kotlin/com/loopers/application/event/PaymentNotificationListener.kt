package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentNotificationListener {
    private val log = LoggerFactory.getLogger(PaymentNotificationListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PaymentStatusChangedEvent) {
        log.info(
            "payment-notification orderId={} memberId={} paymentStatus={}",
            event.orderId,
            event.memberId,
            event.paymentStatus,
        )
    }
}
