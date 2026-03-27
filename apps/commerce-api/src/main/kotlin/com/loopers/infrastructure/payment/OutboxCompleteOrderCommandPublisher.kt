package com.loopers.infrastructure.payment

import com.loopers.application.handler.event.payment.CompleteOrderCommandPublisher
import com.loopers.application.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxCompleteOrderCommandPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : CompleteOrderCommandPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(orderId: Long) {
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = orderId.toString(),
            eventType = EVENT_TYPE,
            payload = mapOf("orderId" to orderId),
            partitionKey = orderId.toString(),
            topic = TOPIC,
        )
        log.info("Outbox 주문 완료 커맨드 발행: orderId={}", orderId)
    }

    companion object {
        const val AGGREGATE_TYPE = "ORDER"
        const val EVENT_TYPE = "PaymentSucceeded"
        const val TOPIC = "payment.succeeded"
    }
}
