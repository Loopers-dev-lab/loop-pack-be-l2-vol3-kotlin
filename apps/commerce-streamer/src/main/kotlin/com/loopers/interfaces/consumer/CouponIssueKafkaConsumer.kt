package com.loopers.interfaces.consumer

import com.loopers.application.coupon.CouponIssueRequestEventHandler
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.event.CouponIssueRequestMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueKafkaConsumer(
    private val couponIssueRequestEventHandler: CouponIssueRequestEventHandler,
) {
    @KafkaListener(
        topics = ["\${step3.kafka.coupon-issue-request-topic}"],
        groupId = "\${step3.kafka.coupon-issue-consumer-group}",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun couponIssueRequestListener(
        messages: List<ConsumerRecord<String, CouponIssueRequestMessage>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { message ->
            couponIssueRequestEventHandler.handle(message.value().requestId)
        }
        acknowledgment.acknowledge()
    }
}
