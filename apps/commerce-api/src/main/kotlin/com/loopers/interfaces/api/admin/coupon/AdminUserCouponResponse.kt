package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.UserCouponInfo
import java.time.ZonedDateTime

data class AdminUserCouponResponse(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val usedOrderId: Long?,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: UserCouponInfo): AdminUserCouponResponse {
            return AdminUserCouponResponse(
                id = info.id,
                couponId = info.couponId,
                userId = info.userId,
                status = info.status,
                expiredAt = info.expiredAt,
                usedAt = info.usedAt,
                usedOrderId = info.usedOrderId,
                createdAt = info.createdAt,
            )
        }
    }
}
