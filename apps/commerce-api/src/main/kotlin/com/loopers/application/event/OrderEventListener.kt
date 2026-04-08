package com.loopers.application.event

import com.loopers.domain.event.OrderCreatedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        log.info(
            "[UserAction] ORDER_CREATED userId={} orderId={} totalPrice={} productIds={}",
            event.userId,
            event.orderId,
            event.totalPrice,
            event.productIds,
        )
    }
}
