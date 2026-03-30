package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_issued_coupons_coupon_user",
            columnNames = ["coupon_id", "user_id"],
        ),
    ],
)
class IssuedCoupon(
    couponId: Long,
    userId: Long,
) : BaseEntity() {

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set
}
