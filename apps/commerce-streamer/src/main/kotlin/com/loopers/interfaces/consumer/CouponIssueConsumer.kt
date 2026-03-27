package com.loopers.interfaces.consumer

import com.loopers.application.coupon.IssueCouponFromQueueUseCase
import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopics
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CouponIssueConsumer(
    private val idempotencyService: IdempotencyService,
    private val issueCouponFromQueueUseCase: IssueCouponFromQueueUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.COUPON_ISSUE_REQUESTS],
        groupId = KafkaTopics.GROUP_COUPON_ISSUE,
        containerFactory = KafkaConfig.SINGLE_LISTENER,
    )
    fun consume(
        record: ConsumerRecord<Any, Any>,
        acknowledgment: Acknowledgment,
    ) {
        val generic = record.value() as? GenericRecord
        if (generic == null) {
            log.error(
                "예상과 다른 메시지 타입. topic={}, offset={}, valueType={}",
                record.topic(),
                record.offset(),
                record.value()?.javaClass?.name,
            )
            acknowledgment.acknowledge()
            return
        }
        val requestId = generic["requestId"]?.toString() ?: return
        val eventType = generic["eventType"]?.toString() ?: "COUPON_ISSUE_REQUESTED"

        if (!idempotencyService.tryMarkHandled(requestId, eventType)) {
            log.debug("이미 처리된 쿠폰 발급 요청 skip. requestId={}", requestId)
            acknowledgment.acknowledge()
            return
        }

        val couponId = (generic["couponId"] as Number).toLong()
        val userId = (generic["userId"] as Number).toLong()

        issueCouponFromQueueUseCase.execute(requestId, couponId, userId)
        acknowledgment.acknowledge()
    }
}
