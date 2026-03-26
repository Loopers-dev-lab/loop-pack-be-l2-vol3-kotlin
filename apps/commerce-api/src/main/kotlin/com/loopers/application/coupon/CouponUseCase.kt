package com.loopers.application.coupon

import com.loopers.application.event.OutboxEventWriter
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.coupon.CouponIssueRequestReader
import com.loopers.domain.coupon.CouponIssueRequestRegister
import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.IssuedCouponReader
import com.loopers.kafka.CouponIssueRequestedPayload
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponUseCase(
    private val couponIssueRequestRegister: CouponIssueRequestRegister,
    private val couponIssueRequestReader: CouponIssueRequestReader,
    private val couponReader: CouponReader,
    private val issuedCouponReader: IssuedCouponReader,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val outboxEventWriter: OutboxEventWriter,
) {

    @Transactional
    fun issueCoupon(couponId: Long, memberId: Long): CouponInfo.IssueRequestDetail {
        val coupon = couponReader.getById(couponId)
        coupon.validateIssuable()

        val request = couponIssueRequestRegister.register(couponId, memberId)
        val requestId = requireNotNull(request.id)
        val occurredAt = ZonedDateTime.now()

        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.COUPON_ISSUE_REQUESTED,
                memberId = memberId,
                targetType = "coupon",
                targetId = couponId.toString(),
                details = mapOf("requestId" to requestId),
            ),
        )
        outboxEventWriter.append(
            topic = KafkaTopics.COUPON_ISSUE_REQUESTS,
            event = IntegrationEvent(
                eventId = "coupon-issue-requested:$requestId",
                eventType = "CouponIssueRequested",
                aggregateType = "coupon",
                aggregateId = couponId.toString(),
                key = couponId.toString(),
                version = 1L,
                occurredAt = occurredAt,
                payload = CouponIssueRequestedPayload(
                    requestId = requestId,
                    couponId = couponId,
                    memberId = memberId,
                    requestedAt = occurredAt,
                ),
            ),
        )

        return CouponInfo.IssueRequestDetail.from(request)
    }

    @Transactional(readOnly = true)
    fun getIssueRequest(requestId: Long, memberId: Long): CouponInfo.IssueRequestDetail {
        val request = couponIssueRequestReader.getById(requestId)
        request.validateOwner(memberId)
        return CouponInfo.IssueRequestDetail.from(request)
    }

    @Transactional(readOnly = true)
    fun getMyCoupons(memberId: Long): List<CouponInfo.IssuedDetail> {
        val issuedCoupons = issuedCouponReader.getAllByMemberId(memberId)
        if (issuedCoupons.isEmpty()) return emptyList()

        val couponIds = issuedCoupons.map { it.couponId }.distinct()
        val couponMap = couponReader.getAllByIds(couponIds).associateBy { it.id }

        return issuedCoupons.map { issuedCoupon ->
            val coupon = couponMap[issuedCoupon.couponId] ?: couponReader.getById(issuedCoupon.couponId)
            CouponInfo.IssuedDetail.from(issuedCoupon, coupon)
        }
    }
}
