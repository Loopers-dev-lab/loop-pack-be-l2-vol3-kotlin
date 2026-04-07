package com.loopers.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

/**
 * commerce-streamer 전용 IssuedCoupon 쓰기 모델.
 * 쿠폰 발급 INSERT 용도로만 사용한다.
 * 원본 엔티티는 commerce-api의 IssuedCoupon.
 */
@Entity
@Table(name = "issued_coupons")
class CouponIssuance(
    couponId: Long,
    userId: Long,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long = couponId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "status", nullable = false, length = 20)
    val status: String = "AVAILABLE"

    @Column(name = "version", nullable = false)
    val version: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
