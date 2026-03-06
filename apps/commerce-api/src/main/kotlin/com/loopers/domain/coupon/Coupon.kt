package com.loopers.domain.coupon

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

class Coupon private constructor(
    val id: Long?,
    val name: String,
    val type: Type,
    val discountValue: Long,
    val minOrderAmount: Money?,
    val expiredAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    init {
        when (type) {
            Type.FIXED -> {
                if (discountValue <= 0) {
                    throw CoreException(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
                }
            }
            Type.RATE -> {
                if (discountValue < 1 || discountValue > 100) {
                    throw CoreException(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
                }
            }
        }
    }

    fun isExpired(): Boolean = !ZonedDateTime.now().isBefore(expiredAt)

    fun isDeleted(): Boolean = deletedAt != null

    fun calculateDiscount(orderAmount: Money): Money {
        val discount = when (type) {
            Type.FIXED -> {
                Money(BigDecimal.valueOf(discountValue))
            }
            Type.RATE -> {
                val rawDiscount = orderAmount.amount
                    .multiply(BigDecimal.valueOf(discountValue))
                    .divide(BigDecimal(100))
                    .setScale(0, RoundingMode.CEILING)
                Money(rawDiscount)
            }
        }
        return if (discount.isGreaterThan(orderAmount)) orderAmount else discount
    }

    fun update(
        name: String,
        discountValue: Long,
        minOrderAmount: Money?,
        expiredAt: ZonedDateTime,
    ): Coupon {
        if (expiredAt.isBefore(ZonedDateTime.now())) {
            throw CoreException(ErrorType.COUPON_INVALID_EXPIRATION)
        }
        return Coupon(
            id = id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            deletedAt = deletedAt,
        )
    }

    fun delete(): Coupon = Coupon(
        id = id,
        name = name,
        type = type,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
        deletedAt = ZonedDateTime.now(),
    )

    enum class Type {
        FIXED,
        RATE,
    }

    companion object {
        fun register(
            name: String,
            type: Type,
            discountValue: Long,
            minOrderAmount: Money?,
            expiredAt: ZonedDateTime,
        ): Coupon {
            if (expiredAt.isBefore(ZonedDateTime.now())) {
                throw CoreException(ErrorType.COUPON_INVALID_EXPIRATION)
            }
            return Coupon(
                id = null,
                name = name,
                type = type,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
                deletedAt = null,
            )
        }

        fun retrieve(
            id: Long,
            name: String,
            type: Type,
            discountValue: Long,
            minOrderAmount: Money?,
            expiredAt: ZonedDateTime,
            deletedAt: ZonedDateTime?,
        ): Coupon = Coupon(
            id = id,
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            deletedAt = deletedAt,
        )
    }
}
