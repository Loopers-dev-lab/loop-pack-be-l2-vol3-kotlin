package com.loopers.application.event

import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener(
    private val orderOutboxRepository: OrderOutboxRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleCompleted(event: PaymentEvent.Completed) {
        val outbox = OrderOutbox(
            eventType = "PAYMENT_COMPLETED",
            orderId = event.orderId,
            userId = event.userId,
            totalAmount = event.totalAmount,
        )
        orderOutboxRepository.save(outbox)
        log.info("OrderOutbox 기록: eventType=PAYMENT_COMPLETED, orderId={}", event.orderId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleFailed(event: PaymentEvent.Failed) {
        val outbox = OrderOutbox(
            eventType = "PAYMENT_FAILED",
            orderId = event.orderId,
            userId = event.userId,
            reason = event.reason,
        )
        orderOutboxRepository.save(outbox)
        log.info("OrderOutbox 기록: eventType=PAYMENT_FAILED, orderId={}", event.orderId)
    }
}
