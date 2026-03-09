package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupons",
    indexes = [
        Index(name = "idx_issued_coupons_user_id", columnList = "user_id"),
        Index(name = "idx_issued_coupons_coupon_id", columnList = "coupon_id"),
    ],
)
class IssuedCoupon(
    couponId: Long,
    userId: Long,
    status: IssuedCouponStatus = IssuedCouponStatus.AVAILABLE,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "coupon_id", nullable = false)
    val couponId: Long = couponId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: IssuedCouponStatus = status
        private set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    fun use() {
        validateUsable()
        this.status = IssuedCouponStatus.USED
        this.usedAt = ZonedDateTime.now()
    }

    fun isUsable(): Boolean = status == IssuedCouponStatus.AVAILABLE

    fun validateUsable() {
        if (!isUsable()) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다. 현재 상태: $status")
        }
    }

    fun validateOwner(userId: Long) {
        if (this.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 쿠폰만 사용할 수 있습니다.")
        }
    }
}
