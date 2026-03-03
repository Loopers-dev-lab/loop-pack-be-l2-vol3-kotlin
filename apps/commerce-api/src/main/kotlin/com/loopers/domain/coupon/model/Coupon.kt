package com.loopers.domain.coupon.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.time.ZonedDateTime

class Coupon(
    val id: CouponId = CouponId(0),
    val name: String,
    val type: CouponType,
    val value: Long,
    val maxDiscount: Money? = null,
    val minOrderAmount: Money? = null,
    val totalQuantity: Int? = null,
    issuedCount: Int = 0,
    val expiredAt: ZonedDateTime,
    deletedAt: ZonedDateTime? = null,
) {

    var issuedCount: Int = issuedCount
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    init {
        validate()
    }

    enum class CouponType {
        FIXED,
        RATE,
    }

    fun canIssue(): Boolean =
        !isExpired() && !isDeleted() && (totalQuantity == null || issuedCount < totalQuantity!!)

    fun issue() {
        if (!canIssue()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰을 발급할 수 없습니다.")
        }
        issuedCount++
    }

    fun calculateDiscount(orderAmount: Money): Money {
        return when (type) {
            CouponType.FIXED -> {
                val discount = Money(BigDecimal(value))
                if (discount.value > orderAmount.value) orderAmount else discount
            }
            CouponType.RATE -> {
                val discountValue = orderAmount.value
                    .multiply(BigDecimal(value))
                    .divide(BigDecimal(100), 0, java.math.RoundingMode.HALF_UP)
                val discount = Money(discountValue)
                val cap = maxDiscount
                if (cap != null && discount.value > cap.value) cap else discount
            }
        }
    }

    fun isExpired(): Boolean = expiredAt.isBefore(ZonedDateTime.now())

    fun isDeleted(): Boolean = deletedAt != null

    fun update(
        name: String,
        type: CouponType,
        value: Long,
        maxDiscount: Money?,
        minOrderAmount: Money?,
        totalQuantity: Int?,
        expiredAt: ZonedDateTime,
    ): Coupon {
        return Coupon(
            id = this.id,
            name = name,
            type = type,
            value = value,
            maxDiscount = maxDiscount,
            minOrderAmount = minOrderAmount,
            totalQuantity = totalQuantity,
            issuedCount = this.issuedCount,
            expiredAt = expiredAt,
            deletedAt = this.deletedAt,
        )
    }

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }

    private fun validate() {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 비어있을 수 없습니다.")
        }
        if (value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 값은 0보다 커야 합니다.")
        }
        if (type == CouponType.RATE && (value < 1 || value > 100)) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인율은 1~100 사이여야 합니다.")
        }
        if (totalQuantity != null && totalQuantity!! <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 수량은 0보다 커야 합니다.")
        }
        if (issuedCount < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량은 0 이상이어야 합니다.")
        }
        if (totalQuantity != null && issuedCount > totalQuantity!!) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량이 총 수량을 초과할 수 없습니다.")
        }
    }
}
