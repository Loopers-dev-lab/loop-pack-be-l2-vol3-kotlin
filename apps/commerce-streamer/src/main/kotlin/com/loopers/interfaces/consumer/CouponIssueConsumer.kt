package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.coupon.CouponIssueProcessor
import com.loopers.config.KafkaTopicConfig
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.kafka.RetryableRecordProcessor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val retryableRecordProcessor: RetryableRecordProcessor,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

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
                val eventId = rec.headerValue("eventId")
                val eventType = rec.headerValue("eventType")

                if (eventId == null || eventType == null) {
                    log.warn("이벤트 헤더 누락 [topic={}, offset={}]", rec.topic(), rec.offset())
                    return@processWithRetry
                }

                val payload = objectMapper.readTree(rec.value())
                val requestId = payload.get("requestId").asText()
                val couponId = payload.get("couponId").asLong()
                val userId = payload.get("userId").asLong()

                couponIssueProcessor.process(eventId, eventType, requestId, couponId, userId)
            }
        }
        acknowledgment.acknowledge()
    }

    private fun ConsumerRecord<String, String>.headerValue(key: String): String? {
        return headers().lastHeader(key)?.value()?.let { String(it) }
    }
}
