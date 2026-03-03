package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.CouponErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime
import kotlin.math.min

@Entity
@Table(name = "coupons")
class Coupon private constructor(
    name: String,
    type: CouponType,
    value: Long,
    minOrderAmount: Long?,
    expiredAt: ZonedDateTime,
    deletedAt: ZonedDateTime? = null,
) : BaseEntity() {

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val type: CouponType = type

    @Column(name = "discount_value", nullable = false)
    var value: Long = value
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = deletedAt
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    fun delete() {
        this.deletedAt = ZonedDateTime.now()
    }

    fun update(
        name: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ) {
        validateName(name)
        validateValue(type, value)
        minOrderAmount?.let { validateMinOrderAmount(it) }
        this.name = name.trim()
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
    }

    fun validateApplicable(orderAmount: Money) {
        if (isExpired()) throw CoreException(CouponErrorCode.COUPON_EXPIRED)
        minOrderAmount?.let {
            if (orderAmount.amount < it) {
                throw CoreException(CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET)
            }
        }
    }

    fun calculateDiscount(orderAmount: Money): Money {
        val discount = when (type) {
            CouponType.FIXED -> min(value, orderAmount.amount)
            CouponType.RATE -> orderAmount.amount * value / 100
        }
        return Money(discount)
    }

    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 50

        fun create(
            name: String,
            type: CouponType,
            value: Long,
            minOrderAmount: Long?,
            expiredAt: ZonedDateTime,
        ): Coupon {
            validateName(name)
            validateValue(type, value)
            minOrderAmount?.let { validateMinOrderAmount(it) }
            return Coupon(
                name = name.trim(),
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
        }

        private fun validateName(name: String) {
            val trimmed = name.trim()
            if (trimmed.length < NAME_MIN_LENGTH || trimmed.length > NAME_MAX_LENGTH) {
                throw CoreException(CouponErrorCode.INVALID_COUPON_NAME)
            }
        }

        private fun validateValue(type: CouponType, value: Long) {
            if (value <= 0) throw CoreException(CouponErrorCode.INVALID_COUPON_VALUE)
            if (type == CouponType.RATE && (value < 1 || value > 100)) {
                throw CoreException(CouponErrorCode.INVALID_RATE_VALUE)
            }
        }

        private fun validateMinOrderAmount(amount: Long) {
            if (amount < 0) throw CoreException(CouponErrorCode.INVALID_MIN_ORDER_AMOUNT)
        }
    }
}
