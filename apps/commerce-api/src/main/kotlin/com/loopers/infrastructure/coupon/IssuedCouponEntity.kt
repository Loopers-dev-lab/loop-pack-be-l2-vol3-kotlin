package com.loopers.infrastructure.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_issued_coupons_coupon_member", columnNames = ["coupon_id", "member_id"]),
    ],
    indexes = [
        Index(name = "idx_issued_coupons_member_id", columnList = "member_id"),
        Index(name = "idx_issued_coupons_coupon_id", columnList = "coupon_id"),
    ],
)
class IssuedCouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: ZonedDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
