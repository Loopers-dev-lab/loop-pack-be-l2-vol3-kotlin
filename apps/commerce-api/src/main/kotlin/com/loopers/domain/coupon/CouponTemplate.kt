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
@Table(name = "coupon_templates")
class CouponTemplate(
    name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: CouponType,

    value: Long,

    minOrderAmount: Long? = null,

    expiredAt: ZonedDateTime,

    maxIssuanceCount: Int? = null,
) : BaseEntity() {
    @Column(nullable = false)
    var name: String = name
        protected set

    @Column(nullable = false)
    var value: Long = value
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    @Column(name = "max_issuance_count")
    var maxIssuanceCount: Int? = maxIssuanceCount
        protected set

    init {
        validateValue(type, value)
    }

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    fun calculateDiscount(totalPrice: Long): Long {
        val discount = when (type) {
            CouponType.FIXED -> value
            CouponType.RATE -> totalPrice * value / 100
        }
        return discount.coerceAtMost(totalPrice)
    }

    fun update(name: String, value: Long, minOrderAmount: Long?, expiredAt: ZonedDateTime, maxIssuanceCount: Int? = null) {
        validateValue(type, value)
        this.name = name
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
        this.maxIssuanceCount = maxIssuanceCount
    }

    fun hasIssuanceLimit(): Boolean = maxIssuanceCount != null

    companion object {
        private fun validateValue(type: CouponType, value: Long) {
            if (value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 할인 값은 0보다 커야 합니다.")
            if (type == CouponType.RATE && value > 100) throw CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.")
        }
    }
}
