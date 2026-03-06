package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.ZonedDateTime

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

    fun use() {
        if (used) throw CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.")
        used = true
    }

    fun getStatus(expiredAt: ZonedDateTime): CouponStatus {
        if (used) return CouponStatus.USED
        if (ZonedDateTime.now().isAfter(expiredAt)) return CouponStatus.EXPIRED
        return CouponStatus.AVAILABLE
    }
}
