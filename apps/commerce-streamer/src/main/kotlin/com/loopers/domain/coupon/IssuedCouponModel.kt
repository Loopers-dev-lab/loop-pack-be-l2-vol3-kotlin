package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "issued_coupons")
class IssuedCouponModel(
    couponId: Long,
    userId: Long,
    discountType: DiscountType,
    discountValue: Int,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var discountType: DiscountType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Int = discountValue
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CouponStatus = CouponStatus.AVAILABLE
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set
}
