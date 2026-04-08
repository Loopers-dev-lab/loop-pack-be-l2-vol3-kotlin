package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "issued_coupons")
class IssuedCoupon(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "coupon_template_id", nullable = false)
    val couponTemplateId: Long,
) : BaseEntity() {
    @Version
    var version: Long = 0
        protected set

    @Column(nullable = false)
    var used: Boolean = false
        protected set
}
