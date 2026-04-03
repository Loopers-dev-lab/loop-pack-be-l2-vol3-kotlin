package com.loopers.application.coupon

import com.loopers.config.kafka.KafkaTopics
import com.loopers.domain.coupon.CouponIssueRequest
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Component
class RequestCouponIssueAsyncUseCase(
    private val couponRepository: CouponRepository,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun request(userId: Long, couponId: Long): String {
        val coupon = couponRepository.findById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: $couponId")

        if (coupon.isDeleted()) {
            throw CoreException(ErrorType.BAD_REQUEST, "삭제된 쿠폰입니다.")
        }

        val requestId = UUID.randomUUID().toString()
        val request = CouponIssueRequest.create(
            requestId = requestId,
            couponId = couponId,
            userId = userId,
        )
        couponIssueRequestRepository.save(request)

        val outboxEvent = OutboxEvent.create(
            eventId = requestId,
            eventType = "COUPON_ISSUE_REQUESTED",
            aggregateId = couponId.toString(),
            topic = KafkaTopics.COUPON_ISSUE_REQUESTS,
            partitionKey = couponId.toString(),
            payload = """{"eventType":"COUPON_ISSUE_REQUESTED","requestId":"$requestId","couponId":$couponId,"userId":$userId}""",
            occurredAt = ZonedDateTime.now(),
        )
        outboxEventRepository.save(outboxEvent)

        return requestId
    }
}
