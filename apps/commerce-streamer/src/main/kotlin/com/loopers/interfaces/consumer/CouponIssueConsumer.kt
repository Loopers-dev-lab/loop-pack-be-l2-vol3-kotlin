package com.loopers.interfaces.consumer

import com.loopers.application.CouponIssueProcessor
import com.loopers.event.EventEnvelope
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val couponIssueProcessor: CouponIssueProcessor,
) {

    @KafkaListener(
        topics = ["coupon-issue-requests"],
        groupId = "coupon-issuer",
        containerFactory = "couponIssueListenerContainerFactory",
    )
    fun consume(@Payload envelope: EventEnvelope, ack: Acknowledgment) {
        couponIssueProcessor.process(envelope)
        ack.acknowledge()
    }
}
