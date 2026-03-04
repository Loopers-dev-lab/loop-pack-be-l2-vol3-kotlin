package com.loopers.domain.coupon

import com.loopers.domain.product.Money
import java.time.ZonedDateTime

class UserCoupon private constructor(
    val persistenceId: Long?,
    val refCouponId: Long,
    val refUserId: Long,
    val status: CouponStatus,
    val discountType: DiscountType,
    val discountValue: Long,
    val minOrderAmount: Money,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
) {

    fun use(): UserCoupon {
        if (status != CouponStatus.AVAILABLE) {
            throw CouponException(CouponError.NOT_AVAILABLE, "사용 가능한 상태가 아닙니다. 현재 상태: $status")
        }
        return UserCoupon(
            persistenceId = persistenceId,
            refCouponId = refCouponId,
            refUserId = refUserId,
            status = CouponStatus.USED,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            usedAt = ZonedDateTime.now(),
            issuedAt = issuedAt,
        )
    }

    fun restore(): UserCoupon {
        if (status != CouponStatus.USED) {
            throw CouponException(CouponError.NOT_AVAILABLE, "USED 상태에서만 복원할 수 있습니다. 현재 상태: $status")
        }
        return UserCoupon(
            persistenceId = persistenceId,
            refCouponId = refCouponId,
            refUserId = refUserId,
            status = CouponStatus.AVAILABLE,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            usedAt = null,
            issuedAt = issuedAt,
        )
    }

    fun assertUsableBy(userId: Long, orderAmount: Money) {
        if (refUserId != userId) {
            throw CouponException(CouponError.NOT_OWNED, "본인의 쿠폰이 아닙니다.")
        }
        if (status != CouponStatus.AVAILABLE) {
            throw CouponException(CouponError.NOT_AVAILABLE, "사용 가능한 상태가 아닙니다. 현재 상태: $status")
        }
        if (ZonedDateTime.now().isAfter(expiredAt)) {
            throw CouponException(CouponError.EXPIRED, "만료된 쿠폰입니다.")
        }
        if (orderAmount.amount < minOrderAmount.amount) {
            throw CouponException(
                CouponError.MIN_ORDER_AMOUNT,
                "최소 주문 금액(${minOrderAmount.amount}원)을 충족하지 못했습니다.",
            )
        }
    }

    fun calculateDiscount(orderAmount: Money): Money {
        return when (discountType) {
            DiscountType.FIXED -> Money(discountValue).min(orderAmount)
            DiscountType.RATE -> orderAmount.percentage(discountValue)
        }
    }

    companion object {
        fun issue(coupon: Coupon, userId: Long): UserCoupon {
            val couponId = requireNotNull(coupon.persistenceId) {
                "저장되지 않은 쿠폰으로는 발급할 수 없습니다."
            }
            return UserCoupon(
                persistenceId = null,
                refCouponId = couponId,
                refUserId = userId,
                status = CouponStatus.AVAILABLE,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount,
                expiredAt = coupon.expiredAt,
                usedAt = null,
                issuedAt = ZonedDateTime.now(),
            )
        }

        fun reconstitute(
            persistenceId: Long,
            refCouponId: Long,
            refUserId: Long,
            status: CouponStatus,
            discountType: DiscountType,
            discountValue: Long,
            minOrderAmount: Money,
            expiredAt: ZonedDateTime,
            usedAt: ZonedDateTime?,
            issuedAt: ZonedDateTime,
        ): UserCoupon {
            return UserCoupon(
                persistenceId = persistenceId,
                refCouponId = refCouponId,
                refUserId = refUserId,
                status = status,
                discountType = discountType,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
                usedAt = usedAt,
                issuedAt = issuedAt,
            )
        }
    }
}
