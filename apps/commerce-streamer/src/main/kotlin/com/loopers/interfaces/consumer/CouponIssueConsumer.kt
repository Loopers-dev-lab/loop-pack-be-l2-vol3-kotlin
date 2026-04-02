package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.coupon.CouponIssueProcessor
import com.loopers.config.KafkaTopicConfig
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.kafka.RetryableRecordProcessor
import com.loopers.infrastructure.kafka.requireHeaderValue
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val retryableRecordProcessor: RetryableRecordProcessor,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(
        topics = [KafkaTopicConfig.COUPON_ISSUE_REQUESTS],
        groupId = "commerce-streamer-coupon",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            retryableRecordProcessor.processWithRetry(record) { rec ->
                val eventId = rec.requireHeaderValue("eventId")
                val eventType = rec.requireHeaderValue("eventType")

                val payload = objectMapper.readTree(rec.value())
                val requestId = payload.get("requestId")?.asText()
                    ?: throw IllegalArgumentException("필수 필드 누락: requestId")
                val couponId = payload.get("couponId")?.asLong()
                    ?: throw IllegalArgumentException("필수 필드 누락: couponId")
                val userId = payload.get("userId")?.asLong()
                    ?: throw IllegalArgumentException("필수 필드 누락: userId")

                couponIssueProcessor.process(eventId, eventType, requestId, couponId, userId)
            }
        }
        acknowledgment.acknowledge()
    }
}
