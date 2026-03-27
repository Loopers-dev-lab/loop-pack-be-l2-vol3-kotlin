package com.loopers.infrastructure.payment

import com.loopers.application.handler.payment.CompleteOrderCommandPublisher
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
            aggregateType = "ORDER",
            aggregateId = orderId.toString(),
            eventType = "PaymentSucceeded",
            payload = mapOf("orderId" to orderId),
            partitionKey = orderId.toString(),
            topic = PAYMENT_SUCCEEDED_TOPIC,
        )
        log.info("Outbox 주문 완료 커맨드 발행: orderId={}", orderId)
    }

    companion object {
        const val PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded"
    }
}
