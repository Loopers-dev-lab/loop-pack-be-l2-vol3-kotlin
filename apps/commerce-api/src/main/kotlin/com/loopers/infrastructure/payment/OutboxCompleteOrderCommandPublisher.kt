package com.loopers.infrastructure.payment

import com.loopers.application.handler.event.payment.CompleteOrderCommandPublisher
import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.event.EventContract
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxCompleteOrderCommandPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : CompleteOrderCommandPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(orderId: Long) {
        outboxEventPublisher.publish(
            aggregateType = EventContract.AGGREGATE_ORDER,
            aggregateId = orderId.toString(),
            eventType = EventContract.EVENT_PAYMENT_SUCCEEDED,
            payload = mapOf("orderId" to orderId),
            partitionKey = orderId.toString(),
            topic = EventContract.PAYMENT_SUCCEEDED_TOPIC,
        )
        log.info("Outbox 주문 완료 커맨드 발행: orderId={}", orderId)
    }
}
