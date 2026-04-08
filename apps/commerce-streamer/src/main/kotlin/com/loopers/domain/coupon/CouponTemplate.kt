package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon_templates")
class CouponTemplate(
    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: CouponType,

    @Column(nullable = false)
    val value: Long,

    @Column(name = "min_order_amount")
    val minOrderAmount: Long? = null,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime,

    @Column(name = "max_issuance_count")
    val maxIssuanceCount: Int? = null,
) : BaseEntity()

enum class CouponType {
    FIXED,
    RATE,
}
