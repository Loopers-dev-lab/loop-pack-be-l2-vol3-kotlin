package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import org.hibernate.annotations.Comment
import java.time.ZonedDateTime

/**
 * 쿠폰 발급 엔티티 — streamer 관점.
 *
 * commerce-api의 coupon_issues 테이블과 동일한 테이블을 가리킨다.
 * streamer는 발급(INSERT)만 수행하므로, 발급에 필요한 필드와 행위만 노출한다.
 *
 * 왜 별도로 정의하는가?
 * - commerce-api와 commerce-streamer는 별도 Spring Boot 앱
 * - 공유 모듈이 없는 현재 구조에서 엔티티를 참조할 수 없음
 * - streamer가 필요한 최소한의 인터페이스만 노출하여 결합도를 낮춤
 * - 향후 MSA 전환 시 이미 분리되어 있어 이동 비용이 적음
 */
@Entity
@Table(
    name = "coupon_issues",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_id"])],
)
@Comment("발급된 쿠폰")
class CouponIssue(
    couponId: Long,
    userId: Long,
) : BaseEntity() {

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CouponIssueStatus = CouponIssueStatus.AVAILABLE
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set
}

enum class CouponIssueStatus {
    AVAILABLE,
    USED,
    EXPIRED,
}
