package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

/**
 * 쿠폰 발급 요청 엔티티 — streamer 관점.
 *
 * commerce-api의 coupon_issue_request 테이블과 동일한 테이블을 가리킨다.
 * streamer는 요청 상태 업데이트(UPDATE)만 수행하므로,
 * 상태 변경에 필요한 필드와 행위만 노출한다.
 *
 * 생성(INSERT)은 commerce-api가 담당하므로 생성자에 비즈니스 파라미터를 두지 않는다.
 */
@Entity
@Table(name = "coupon_issue_request")
@Comment("선착순 쿠폰 발급 요청")
class CouponIssueRequest : BaseEntity() {

    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    var requestId: String = ""
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: IssueRequestStatus = IssueRequestStatus.PENDING
        protected set

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
