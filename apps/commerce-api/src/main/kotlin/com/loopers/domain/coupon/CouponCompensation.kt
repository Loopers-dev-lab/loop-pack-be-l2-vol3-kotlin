package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

/**
 * 쿠폰 USED 마킹 실패 시 보상 처리를 위한 로그 엔티티.
 *
 * OrderEventListener에서 재시도 소진 후 PENDING 상태로 생성되며,
 * CouponCompensationScheduler가 주기적으로 재시도한다.
 */
@Entity
@Table(name = "coupon_compensation_log")
@Comment("쿠폰 보상 처리 로그")
class CouponCompensation(
    orderId: Long,
    couponIssueId: Long,
    eventId: String,
) : BaseEntity() {

    @Comment("주문 ID")
    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Comment("발급 쿠폰 ID")
    @Column(name = "coupon_issue_id", nullable = false)
    var couponIssueId: Long = couponIssueId
        protected set

    @Comment("원본 이벤트 ID")
    @Column(name = "event_id", nullable = false)
    var eventId: String = eventId
        protected set

    @Comment("보상 처리 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CompensationStatus = CompensationStatus.PENDING
        protected set

    @Comment("재시도 횟수")
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        protected set

    fun markResolved() {
        this.status = CompensationStatus.RESOLVED
    }

    fun markFailed() {
        this.status = CompensationStatus.FAILED
    }

    fun incrementRetry() {
        this.retryCount++
    }
}
