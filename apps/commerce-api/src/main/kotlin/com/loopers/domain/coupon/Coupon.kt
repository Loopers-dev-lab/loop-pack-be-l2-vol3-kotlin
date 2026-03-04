package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class Coupon(
    name: String,
    type: CouponType,
    value: BigDecimal,
    minOrderAmount: BigDecimal?,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "name", nullable = false, length = 200)
    var name: String = name
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: CouponType = type

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    var value: BigDecimal = value
        private set

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    var minOrderAmount: BigDecimal? = minOrderAmount
        private set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        private set

    init {
        validateValue(value, type)
    }

    fun update(
        name: String,
        value: BigDecimal,
        minOrderAmount: BigDecimal?,
        expiredAt: ZonedDateTime,
    ) {
        validateValue(value, type)
        this.name = name
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
    }

    fun calculateDiscount(orderAmount: BigDecimal): BigDecimal {
        val discount = when (type) {
            CouponType.FIXED -> value
            CouponType.RATE -> orderAmount.multiply(value).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        }
        return discount.min(orderAmount)
    }

    fun isExpired(): Boolean = expiredAt.isBefore(ZonedDateTime.now())

    fun isDeleted(): Boolean = deletedAt != null

    fun validateMinOrderAmount(orderAmount: BigDecimal) {
        val min = minOrderAmount ?: return
        if (orderAmount < min) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액은 ${min}원입니다.")
        }
    }

    private fun validateValue(value: BigDecimal, type: CouponType) {
        if (value <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 할인 값은 0보다 커야 합니다.")
        }
        if (type == CouponType.RATE && value > BigDecimal("100")) {
            throw CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.")
        }
    }
}
