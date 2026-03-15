package com.loopers.domain.coupon

import com.loopers.domain.product.Money
import java.time.ZonedDateTime

class Coupon private constructor(
    val persistenceId: Long?,
    val name: CouponName,
    val discountType: DiscountType,
    val discountValue: Long,
    val minOrderAmount: Money,
    val maxIssueCount: Int?,
    val issuedCount: Int,
    val expiredAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    fun isDeleted(): Boolean = deletedAt != null

    fun canIssue(): Boolean {
        if (isExpired()) return false
        val max = maxIssueCount ?: return true
        return issuedCount < max
    }

    fun assertIssuable() {
        if (isExpired()) {
            throw CouponException(CouponError.EXPIRED, "만료된 쿠폰입니다.")
        }
        val max = maxIssueCount
        if (max != null && issuedCount >= max) {
            throw CouponException(CouponError.MAX_ISSUED, "발급 수량이 초과되었습니다.")
        }
    }

    fun update(
        name: CouponName,
        discountType: DiscountType,
        discountValue: Long,
        minOrderAmount: Money,
        maxIssueCount: Int?,
        expiredAt: ZonedDateTime,
    ): Coupon {
        validateDiscountValue(discountType, discountValue)
        return Coupon(
            persistenceId = persistenceId,
            name = name,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxIssueCount = maxIssueCount,
            issuedCount = issuedCount,
            expiredAt = expiredAt,
            deletedAt = deletedAt,
        )
    }

    fun delete(): Coupon {
        return Coupon(
            persistenceId = persistenceId,
            name = name,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            maxIssueCount = maxIssueCount,
            issuedCount = issuedCount,
            expiredAt = expiredAt,
            deletedAt = ZonedDateTime.now(),
        )
    }

    companion object {
        fun create(
            name: CouponName,
            discountType: DiscountType,
            discountValue: Long,
            minOrderAmount: Money,
            maxIssueCount: Int?,
            expiredAt: ZonedDateTime,
        ): Coupon {
            validateDiscountValue(discountType, discountValue)
            return Coupon(
                persistenceId = null,
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                maxIssueCount = maxIssueCount,
                issuedCount = 0,
                expiredAt = expiredAt,
                deletedAt = null,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            name: CouponName,
            discountType: DiscountType,
            discountValue: Long,
            minOrderAmount: Money,
            maxIssueCount: Int?,
            issuedCount: Int,
            expiredAt: ZonedDateTime,
            deletedAt: ZonedDateTime?,
        ): Coupon {
            return Coupon(
                persistenceId = persistenceId,
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                maxIssueCount = maxIssueCount,
                issuedCount = issuedCount,
                expiredAt = expiredAt,
                deletedAt = deletedAt,
            )
        }

        private fun validateDiscountValue(discountType: DiscountType, discountValue: Long) {
            when (discountType) {
                DiscountType.FIXED -> require(discountValue > 0) {
                    "정액 할인 금액은 0보다 커야 합니다."
                }
                DiscountType.RATE -> require(discountValue in 1..100) {
                    "정률 할인율은 1~100 사이여야 합니다."
                }
            }
        }
    }
}
