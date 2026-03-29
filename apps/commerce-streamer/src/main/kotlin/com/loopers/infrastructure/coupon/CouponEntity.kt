package com.loopers.infrastructure.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class CouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime,

    @Column(name = "issue_limit")
    val issueLimit: Long? = null,

    @Column(name = "issued_count", nullable = false)
    var issuedCount: Long = 0L,
)
