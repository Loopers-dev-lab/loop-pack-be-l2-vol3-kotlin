package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.coupon.CouponIssueRequestedEventHandler
import com.loopers.infrastructure.outbox.CouponIssueRequestedOutboxMessagePayload
import com.loopers.infrastructure.outbox.KafkaEventType
import com.loopers.infrastructure.outbox.KafkaOutboxEnvelope
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandler: CouponIssueRequestedEventHandler,
) {
    @KafkaListener(
        topics = ["coupon-issue-requests"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        records: List<ConsumerRecord<String, ByteArray>>,
        acknowledgment: Acknowledgment,
    ) {
        records.forEach { record ->
            val envelope = objectMapper.readValue(record.value(), KafkaOutboxEnvelope::class.java)
            if (envelope.eventType != KafkaEventType.COUPON_ISSUE_REQUESTED) {
                throw IllegalStateException("Unexpected event type: ${envelope.eventType}")
            }
            val payload = objectMapper.treeToValue(
                envelope.payload,
                CouponIssueRequestedOutboxMessagePayload::class.java,
            )
            eventHandler.handle(payload)
        }
        acknowledgment.acknowledge()
    }
}
