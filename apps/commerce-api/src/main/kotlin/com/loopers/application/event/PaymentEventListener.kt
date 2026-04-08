package com.loopers.application.event

import com.loopers.domain.event.PaymentCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        log.info(
            "[UserAction] PAYMENT_COMPLETED userId={} orderId={} paymentId={} amount={}",
            event.userId,
            event.orderId,
            event.paymentId,
            event.amount,
        )
    }
}
