package com.loopers.infrastructure.payment

import com.loopers.application.handler.event.payment.PaymentCompensationPublisher
import com.loopers.application.outbox.OutboxEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OutboxPaymentCompensationPublisher(
    private val outboxEventPublisher: OutboxEventPublisher,
) : PaymentCompensationPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(orderId: Long, paymentId: Long) {
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = orderId.toString(),
            eventType = EVENT_TYPE,
            payload = mapOf("orderId" to orderId, "paymentId" to paymentId),
            partitionKey = orderId.toString(),
            topic = TOPIC,
        )
        log.info("Outbox 결제 실패 보상 커맨드 발행: orderId={}, paymentId={}", orderId, paymentId)
    }

    companion object {
        const val AGGREGATE_TYPE = "ORDER"
        const val EVENT_TYPE = "PaymentFailed"
        const val TOPIC = "payment.failed"
    }
}
