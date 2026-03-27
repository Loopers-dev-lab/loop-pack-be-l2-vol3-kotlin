package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.util.UUID

/**
 * 선착순 쿠폰 발급 요청.
 *
 * API에서 Kafka로 발행 시 생성되며, Consumer가 처리 후 상태를 업데이트한다.
 * 유저가 GET /coupons/issue-request/{requestId}로 결과를 조회한다.
 */
@Entity
@Table(
    name = "coupon_issue_request",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_id"])],
)
@Comment("선착순 쿠폰 발급 요청")
class CouponIssueRequest(
    userId: Long,
    couponId: Long,
) : BaseEntity() {

    @Comment("요청 식별자 (클라이언트 폴링용)")
    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    var requestId: String = UUID.randomUUID().toString()
        protected set

    @Comment("요청 유저")
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Comment("대상 쿠폰")
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Comment("처리 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: IssueRequestStatus = IssueRequestStatus.PENDING
        protected set

    @Comment("실패 사유")
    @Column(name = "failure_reason")
    var failureReason: String? = null
        protected set

    fun markSuccess() {
        this.status = IssueRequestStatus.SUCCESS
    }

    fun markFailed(reason: String) {
        this.status = IssueRequestStatus.FAILED
        this.failureReason = reason
    }

    fun markExhausted() {
        this.status = IssueRequestStatus.EXHAUSTED
        this.failureReason = "수량 소진"
    }
}

enum class IssueRequestStatus {
    PENDING,
    SUCCESS,
    FAILED,
    EXHAUSTED,
}
