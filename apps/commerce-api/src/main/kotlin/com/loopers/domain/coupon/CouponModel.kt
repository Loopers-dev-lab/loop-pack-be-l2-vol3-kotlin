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
class CouponModel(
    name: String,
    discountType: DiscountType,
    discountValue: Int,
    totalQuantity: Int,
    issuedQuantity: Int = 0,
    expiredAt: ZonedDateTime,
) : BaseEntity() {
    @Column(nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var discountType: DiscountType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Int = discountValue
        protected set

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = totalQuantity
        protected set

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = issuedQuantity
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    init {
        validateName(name)
        validateDiscountValue(discountType, discountValue)
        validateTotalQuantity(totalQuantity)
        validateIssuedQuantity(issuedQuantity, totalQuantity)
    }

    fun issue() {
        if (isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        if (issuedQuantity >= totalQuantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급 수량이 초과되었습니다.")
        }
        issuedQuantity++
    }

    fun calculateDiscount(originalPrice: BigDecimal): BigDecimal {
        return when (discountType) {
            DiscountType.FIXED -> originalPrice.min(BigDecimal(discountValue))
            DiscountType.PERCENTAGE -> originalPrice.multiply(BigDecimal(discountValue))
                .divide(BigDecimal(100), 2, RoundingMode.FLOOR)
        }
    }

    fun isExpired(): Boolean {
        return expiredAt.isBefore(ZonedDateTime.now())
    }

    fun update(
        newName: String,
        newDiscountType: DiscountType,
        newDiscountValue: Int,
        newTotalQuantity: Int,
        newExpiredAt: ZonedDateTime,
    ) {
        validateName(newName)
        validateDiscountValue(newDiscountType, newDiscountValue)
        validateTotalQuantity(newTotalQuantity)
        validateIssuedQuantity(issuedQuantity, newTotalQuantity)
        this.name = newName
        this.discountType = newDiscountType
        this.discountValue = newDiscountValue
        this.totalQuantity = newTotalQuantity
        this.expiredAt = newExpiredAt
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 비어있을 수 없습니다.")
        }
    }

    private fun validateDiscountValue(type: DiscountType, value: Int) {
        if (value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.")
        }
        if (type == DiscountType.PERCENTAGE && value > 100) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인율은 100을 초과할 수 없습니다.")
        }
    }

    private fun validateTotalQuantity(totalQuantity: Int) {
        if (totalQuantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 발급 수량은 0보다 커야 합니다.")
        }
    }

    private fun validateIssuedQuantity(issuedQuantity: Int, totalQuantity: Int) {
        if (issuedQuantity > totalQuantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량이 총 수량을 초과할 수 없습니다.")
        }
    }
}
