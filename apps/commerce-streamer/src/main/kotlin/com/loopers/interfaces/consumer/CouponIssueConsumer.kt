package com.loopers.interfaces.consumer

import com.loopers.application.coupon.IssueCouponFromQueueUseCase
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
        // poison pill은 예외를 던져서 DefaultErrorHandler → DLT 경로를 타도록 한다.
        // ack로 폐기하면 운영자가 장애를 인지할 수 없고 재처리도 불가능하다.
        val generic = record.value() as? GenericRecord
            ?: throw IllegalArgumentException(
                "예상과 다른 메시지 타입. topic=${record.topic()}, offset=${record.offset()}, " +
                    "valueType=${record.value()?.javaClass?.name}",
            )

        val requestId = generic["requestId"]?.toString()
            ?: throw IllegalArgumentException(
                "requestId가 없는 메시지. topic=${record.topic()}, offset=${record.offset()}",
            )

        val eventType = generic["eventType"]?.toString() ?: "COUPON_ISSUE_REQUESTED"

        val couponId = (generic["couponId"] as? Number)?.toLong()
            ?: throw IllegalArgumentException(
                "couponId가 유효하지 않습니다. topic=${record.topic()}, offset=${record.offset()}",
            )

        val userId = (generic["userId"] as? Number)?.toLong()
            ?: throw IllegalArgumentException(
                "userId가 유효하지 않습니다. topic=${record.topic()}, offset=${record.offset()}",
            )

        issueCouponFromQueueUseCase.execute(requestId, eventType, couponId, userId)
        acknowledgment.acknowledge()
    }
}
