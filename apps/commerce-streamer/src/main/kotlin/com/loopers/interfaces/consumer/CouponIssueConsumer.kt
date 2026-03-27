package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.coupon.CouponIssueProcessor
import com.loopers.domain.event.EventHandled
import com.loopers.domain.event.EventHandledRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * 선착순 쿠폰 발급 컨슈머.
 *
 * coupon-issue-requests 토픽을 소비하여 실제 쿠폰을 발급한다.
 * - RECORD_LISTENER (concurrency=1): 파티션 내 순서 보장 + 건별 에러 핸들링
 * - DefaultErrorHandler: 실패 시 3회 재시도(1초 간격) 후 DLQ로 자동 격리
 * - key=couponId → 같은 쿠폰 요청은 같은 파티션에서 순차 처리
 * - event_handled 기반 멱등 처리
 * - 수량 체크: atomic UPDATE (issued_count < max_issue_count)
 * - 중복 발급 방지: coupon_issues 테이블 UK(user_id, coupon_id)
 */
@Component
class CouponIssueConsumer(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val eventHandledRepository: EventHandledRepository,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(CouponIssueConsumer::class.java)
        private const val GROUP_ID = "coupon-issue-consumer"
    }

    @KafkaListener(
        topics = ["coupon-issue-requests"],
        groupId = GROUP_ID,
        containerFactory = KafkaConfig.RECORD_LISTENER,
    )
    fun consume(
        record: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val payload = objectMapper.readTree(record.value())
        val requestId = payload.get("requestId")?.asText()
        val userId = payload.get("userId")?.asLong()
        val couponId = payload.get("aggregateId")?.asLong()

        if (requestId == null || userId == null || couponId == null) {
            log.warn("쿠폰 발급 메시지 파싱 실패 — skip [offset={}]", record.offset())
            acknowledgment.acknowledge()
            return
        }

        // 멱등 체크
        if (eventHandledRepository.existsByEventId(requestId)) {
            log.debug("이미 처리된 발급 요청 skip [requestId={}]", requestId)
            acknowledgment.acknowledge()
            return
        }

        couponIssueProcessor.process(requestId, userId, couponId)
        eventHandledRepository.save(EventHandled(eventId = requestId))
        log.info("[쿠폰 발급] 처리 완료 [requestId={}, userId={}, couponId={}]", requestId, userId, couponId)
        acknowledgment.acknowledge()
    }
}
