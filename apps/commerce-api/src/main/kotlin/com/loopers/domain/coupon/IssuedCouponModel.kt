package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
    status: CouponStatus = CouponStatus.AVAILABLE,
) : BaseEntity() {
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CouponStatus = status
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    init {
        if (couponId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 0보다 커야 합니다.")
        }
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 0보다 커야 합니다.")
        }
    }

    fun use() {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 가능한 상태의 쿠폰이 아닙니다.")
        }
        status = CouponStatus.USED
        usedAt = ZonedDateTime.now()
    }

    fun expire() {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 가능한 상태의 쿠폰만 만료 처리할 수 있습니다.")
        }
        status = CouponStatus.EXPIRED
    }

    fun restoreUsage() {
        if (status != CouponStatus.USED) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용된 상태의 쿠폰만 복구할 수 있습니다.")
        }
        status = CouponStatus.AVAILABLE
        usedAt = null
    }
}
