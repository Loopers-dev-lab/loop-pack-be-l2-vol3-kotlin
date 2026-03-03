package com.loopers.domain.coupon.model

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.time.ZonedDateTime

class Coupon(
    val id: CouponId = CouponId(0),
    name: String,
    type: CouponType,
    value: Long,
    maxDiscount: Money? = null,
    minOrderAmount: Money? = null,
    totalQuantity: Int? = null,
    issuedCount: Int = 0,
    expiredAt: ZonedDateTime,
    deletedAt: ZonedDateTime? = null,
) {

    var name: String = name
        private set

    var type: CouponType = type
        private set

    var value: Long = value
        private set

    var maxDiscount: Money? = maxDiscount
        private set

    var minOrderAmount: Money? = minOrderAmount
        private set

    var totalQuantity: Int? = totalQuantity
        private set

    var issuedCount: Int = issuedCount
        private set

    var expiredAt: ZonedDateTime = expiredAt
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
    ) {
        this.name = name
        this.type = type
        this.value = value
        this.maxDiscount = maxDiscount
        this.minOrderAmount = minOrderAmount
        this.totalQuantity = totalQuantity
        this.expiredAt = expiredAt
        validate()
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
