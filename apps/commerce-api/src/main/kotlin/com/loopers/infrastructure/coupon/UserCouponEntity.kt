package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "user_coupon")
class UserCouponEntity(
    id: Long?,

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: CouponStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    val discountType: DiscountType,

    @Column(name = "discount_value", nullable = false)
    val discountValue: Long,

    @Column(name = "min_order_amount", nullable = false)
    val minOrderAmount: Long,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime,

    @Column(name = "used_at")
    val usedAt: ZonedDateTime?,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: ZonedDateTime,
) : BaseEntity() {
    init {
        this.id = id
    }
}
