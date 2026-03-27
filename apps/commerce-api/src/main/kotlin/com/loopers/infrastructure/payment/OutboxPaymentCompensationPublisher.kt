package com.loopers.infrastructure.payment

import com.loopers.application.handler.payment.PaymentCompensationPublisher
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
            aggregateType = "ORDER",
            aggregateId = orderId.toString(),
            eventType = "PaymentFailed",
            payload = mapOf("orderId" to orderId, "paymentId" to paymentId),
            partitionKey = orderId.toString(),
            topic = PAYMENT_FAILED_TOPIC,
        )
        log.info("Outbox 결제 실패 보상 커맨드 발행: orderId={}, paymentId={}", orderId, paymentId)
    }

    companion object {
        const val PAYMENT_FAILED_TOPIC = "payment.failed"
    }
}
