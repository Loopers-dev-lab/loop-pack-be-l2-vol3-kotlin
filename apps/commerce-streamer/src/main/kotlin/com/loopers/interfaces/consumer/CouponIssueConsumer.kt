package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.service.CouponIssueService
import com.loopers.application.service.DlqHandler
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.event.CouponIssueRequestedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val couponIssueService: CouponIssueService,
    private val dlqHandler: DlqHandler,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["coupon-limited-events", "coupon-normal-events"],
        containerFactory = KafkaConfig.COUPON_LISTENER,
    )
    fun handleCouponIssueEvents(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        for (message in messages) {
            try {
                val payload = message.value() as String
                val event = objectMapper.readValue(payload, CouponIssueRequestedEvent::class.java)
                couponIssueService.processIssuanceRequest(event)
            } catch (e: Exception) {
                val msg = "Failed to process coupon issue message from topic=${message.topic()}, " +
                    "partition=${message.partition()}, offset=${message.offset()}"
                logger.error(msg, e)
                // DLQ에 저장 후 skip — 재소비 방지를 위해 ACK는 배치 전체 처리 후 수행
                val payload = message.value() as? String ?: ""
                dlqHandler.saveToDlq(
                    originalTopic = message.topic(),
                    messagePayload = payload,
                    consumerGroup = "commerce-streamer-coupon",
                    eventType = "CouponIssueRequestedEvent",
                    exception = e,
                )
            }
        }
        // 성공/실패 여부와 관계없이 ACK — DLQ에 저장된 메시지는 재소비하지 않음
        acknowledgment.acknowledge()
    }
}
