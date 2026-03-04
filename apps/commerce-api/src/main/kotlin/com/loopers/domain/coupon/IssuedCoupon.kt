package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = [UniqueConstraint(name = "uk_issued_coupons_coupon_user", columnNames = ["coupon_id", "user_id"])],
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

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    init {
        if (couponId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 양수여야 합니다.")
        }
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 양수여야 합니다.")
        }
    }

    fun use() {
        usedAt = ZonedDateTime.now()
    }

    fun status(couponExpiresAt: ZonedDateTime): IssuedCouponStatus = when {
        usedAt != null -> IssuedCouponStatus.USED
        couponExpiresAt.isBefore(ZonedDateTime.now()) -> IssuedCouponStatus.EXPIRED
        else -> IssuedCouponStatus.AVAILABLE
    }
}
