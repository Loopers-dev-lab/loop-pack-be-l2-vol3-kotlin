package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.ZonedDateTime

@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_id"])],
)
class UserCoupon private constructor(
    couponId: Long,
    userId: Long,
    expiredAt: ZonedDateTime,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long = couponId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: UserCouponStatus = UserCouponStatus.AVAILABLE
        protected set

    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime = expiredAt

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    @Column(name = "used_order_id")
    var usedOrderId: Long? = null
        protected set

    @Version
    @Column(nullable = false)
    var version: Long = 0
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    fun validateUsableBy(userId: Long) {
        if (this.userId != userId) throw CoreException(CouponErrorCode.COUPON_NOT_OWNED)
        if (this.status != UserCouponStatus.AVAILABLE) throw CoreException(CouponErrorCode.COUPON_ALREADY_USED)
        if (isExpired()) throw CoreException(CouponErrorCode.COUPON_EXPIRED)
    }

    fun use(orderId: Long) {
        this.status = UserCouponStatus.USED
        this.usedAt = ZonedDateTime.now()
        this.usedOrderId = orderId
    }

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            couponId: Long,
            userId: Long,
            expiredAt: ZonedDateTime,
        ): UserCoupon {
            return UserCoupon(
                couponId = couponId,
                userId = userId,
                expiredAt = expiredAt,
            )
        }
    }
}
