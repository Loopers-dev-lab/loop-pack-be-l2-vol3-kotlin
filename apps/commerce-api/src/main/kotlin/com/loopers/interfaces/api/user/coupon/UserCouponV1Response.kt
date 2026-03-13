package com.loopers.interfaces.api.user.coupon

import com.loopers.application.user.coupon.UserCouponResult
import java.time.ZonedDateTime

class UserCouponV1Response {
    data class Issued(
        val id: Long,
        val couponId: Long,
        val status: String,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: UserCouponResult.Issued): Issued = Issued(
                id = result.id,
                couponId = result.couponId,
                status = result.status,
                expiredAt = result.expiredAt,
            )
        }
    }

    data class ListItem(
        val id: Long,
        val couponId: Long,
        val couponName: String,
        val couponType: String,
        val discountValue: Long,
        val displayStatus: String,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(result: UserCouponResult.ListItem): ListItem = ListItem(
                id = result.id,
                couponId = result.couponId,
                couponName = result.couponName,
                couponType = result.couponType,
                discountValue = result.discountValue,
                displayStatus = result.displayStatus,
                expiredAt = result.expiredAt,
                usedAt = result.usedAt,
            )
        }
    }
}
