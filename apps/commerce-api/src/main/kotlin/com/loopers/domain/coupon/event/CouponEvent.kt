package com.loopers.domain.coupon.event

import com.loopers.domain.event.DomainEvent
import com.loopers.domain.event.EventType

sealed class CouponEvent : DomainEvent() {

    /**
     * 선착순 쿠폰 발급 요청 이벤트.
     *
     * CouponFacade에서 발행 → OutboxEventListener가 outbox에 저장
     * → OutboxEventRelay가 coupon-issue-requests 토픽으로 발행
     * → CouponIssueConsumer(streamer)가 소비하여 실제 발급 처리
     */
    data class IssueRequested(
        override val aggregateId: Long, // couponId
        val requestId: String,
        val userId: Long,
    ) : CouponEvent() {
        override val eventType: EventType = EventType.COUPON_ISSUE_REQUESTED
    }
}
