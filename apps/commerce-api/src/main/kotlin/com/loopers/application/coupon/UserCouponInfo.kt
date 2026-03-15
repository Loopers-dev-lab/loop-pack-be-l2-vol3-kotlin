package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCoupon
import java.time.ZonedDateTime

data class UserCouponInfo(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val issuedAt: ZonedDateTime,
) {
    companion object {
        fun from(userCoupon: UserCoupon): UserCouponInfo {
            val id = requireNotNull(userCoupon.persistenceId) {
                "UserCoupon.persistenceId가 null입니다. 저장된 UserCoupon만 매핑 가능합니다."
            }
            return UserCouponInfo(
                id = id,
                couponId = userCoupon.refCouponId,
                userId = userCoupon.refUserId,
                status = userCoupon.status.name,
                discountType = userCoupon.discountType.name,
                discountValue = userCoupon.discountValue,
                minOrderAmount = userCoupon.minOrderAmount.amount,
                expiredAt = userCoupon.expiredAt,
                usedAt = userCoupon.usedAt,
                issuedAt = userCoupon.issuedAt,
            )
        }
    }
}
