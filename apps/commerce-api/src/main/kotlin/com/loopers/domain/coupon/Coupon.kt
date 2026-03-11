package com.loopers.domain.coupon

import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime
import kotlin.math.min

class Coupon(
    val id: Long? = null,
    name: CouponName,
    val type: CouponType,
    val discountValue: DiscountValue,
    val minOrderAmount: MinOrderAmount,
    val expiredAt: ZonedDateTime,
) {
    var name: CouponName = name
        private set

    init {
        if (type == CouponType.RATE && discountValue.value > 100) {
            throw CoreException(ErrorType.INVALID_COUPON_VALUE)
        }
    }

    fun changeInfo(
        name: CouponName,
        type: CouponType,
        discountValue: DiscountValue,
        minOrderAmount: MinOrderAmount,
        expiredAt: ZonedDateTime,
    ): Coupon {
        return Coupon(
            id = this.id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
    }

    fun isExpired(): Boolean {
        return expiredAt.isBefore(ZonedDateTime.now())
    }

    fun validateIssuable() {
        if (isExpired()) {
            throw CoreException(ErrorType.COUPON_EXPIRED)
        }
    }

    fun validateApplicable(totalPrice: Long) {
        if (isExpired()) {
            throw CoreException(ErrorType.COUPON_EXPIRED)
        }
        minOrderAmount.value?.let {
            if (totalPrice < it) {
                throw CoreException(ErrorType.COUPON_MIN_ORDER_AMOUNT_NOT_MET)
            }
        }
    }

    fun calculateDiscount(originalPrice: Long): Long {
        return when (type) {
            CouponType.FIXED -> min(discountValue.value, originalPrice)
            CouponType.RATE -> originalPrice * discountValue.value / 100
        }
    }
}
