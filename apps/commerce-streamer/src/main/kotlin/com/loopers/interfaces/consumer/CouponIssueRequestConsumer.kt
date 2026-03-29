package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.consumer.DeadLetterPublisher
import com.loopers.application.consumer.EventHandledRecorder
import com.loopers.application.consumer.RawIntegrationEvent
import com.loopers.application.coupon.CouponIssueRequestProcessor
import com.loopers.config.kafka.KafkaConfig
import com.loopers.kafka.CouponIssueRequestedPayload
import com.loopers.kafka.KafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestConsumer(
    private val objectMapper: ObjectMapper,
    private val eventHandledRecorder: EventHandledRecorder,
    private val couponIssueRequestProcessor: CouponIssueRequestProcessor,
    private val deadLetterPublisher: DeadLetterPublisher,
) {
    companion object {
        private const val CONSUMER_GROUP = "coupon-issue-request-consumer"
    }

    @KafkaListener(
        topics = [KafkaTopics.COUPON_ISSUE_REQUESTS],
        groupId = CONSUMER_GROUP,
        containerFactory = KafkaConfig.MANUAL_LISTENER,
    )
    fun consume(
        message: String,
        acknowledgment: Acknowledgment,
    ) {
        val event = objectMapper.readValue(message, RawIntegrationEvent::class.java)
        if (!eventHandledRecorder.markHandled(CONSUMER_GROUP, event.eventId)) {
            acknowledgment.acknowledge()
            return
        }

        if (event.eventType != "CouponIssueRequested") {
            acknowledgment.acknowledge()
            return
        }

        runCatching {
            val payload = objectMapper.treeToValue(event.payload, CouponIssueRequestedPayload::class.java)
            couponIssueRequestProcessor.process(
                requestId = payload.requestId,
                couponId = payload.couponId,
                memberId = payload.memberId,
            )
        }.onFailure { ex ->
            deadLetterPublisher.publish(
                sourceTopic = KafkaTopics.COUPON_ISSUE_REQUESTS,
                key = event.key,
                payload = message,
                cause = ex,
            )
        }
        acknowledgment.acknowledge()
    }
}
