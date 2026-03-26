package com.loopers.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * commerce-streamer 전용 Coupon 읽기 모델.
 * 쿠폰 발급 시 maxIssueCount, expiredAt 확인 용도로만 사용한다.
 * 원본 엔티티는 commerce-api의 Coupon.
 */
@Entity
@Table(name = "coupons")
class CouponInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "max_issue_count")
    val maxIssueCount: Int? = null

    @Column(name = "expired_at")
    val expiredAt: java.time.ZonedDateTime? = null

    @Column(name = "deleted_at")
    val deletedAt: java.time.ZonedDateTime? = null

    fun isFcfsCoupon(): Boolean = maxIssueCount != null

    fun isExpired(): Boolean = expiredAt?.isBefore(java.time.ZonedDateTime.now()) ?: false

    fun isDeleted(): Boolean = deletedAt != null
}
