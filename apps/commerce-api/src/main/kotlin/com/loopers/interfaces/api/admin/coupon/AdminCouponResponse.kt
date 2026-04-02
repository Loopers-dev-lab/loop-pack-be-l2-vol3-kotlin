package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponInfo
import java.time.ZonedDateTime

data class AdminCouponResponse(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val minOrderAmount: Long?,
    val maxQuantity: Int?,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: CouponInfo): AdminCouponResponse {
            return AdminCouponResponse(
                id = info.id,
                name = info.name,
                type = info.type,
                value = info.value,
                minOrderAmount = info.minOrderAmount,
                maxQuantity = info.maxQuantity,
                expiredAt = info.expiredAt,
                createdAt = info.createdAt,
            )
        }
    }
}
