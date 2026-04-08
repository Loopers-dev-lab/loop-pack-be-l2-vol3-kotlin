package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.deadletter.FailedEventService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

data class CouponIssueMessage(
    val requestId: String = "",
    val userId: Long = 0,
    val couponTemplateId: Long = 0,
)

@Component
class CouponIssueConsumer(
    private val couponIssueService: CouponIssueService,
    private val objectMapper: ObjectMapper,
    private val failedEventService: FailedEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["coupon-issue-requests"],
        groupId = "commerce-streamer-coupon",
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun handleCouponIssueRequest(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val message = objectMapper.readValue(record.value(), CouponIssueMessage::class.java)
            log.info(
                "[CouponIssue] Received: requestId={} userId={} templateId={}",
                message.requestId,
                message.userId,
                message.couponTemplateId,
            )

            couponIssueService.processIssueRequest(
                requestId = message.requestId,
                userId = message.userId,
                couponTemplateId = message.couponTemplateId,
            )
        } catch (e: Exception) {
            log.error("[CouponIssue] Failed to process message: offset={}", record.offset(), e)
            failedEventService.save(record, e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
