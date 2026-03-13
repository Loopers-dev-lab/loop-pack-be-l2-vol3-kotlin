package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.CouponType
import java.time.ZonedDateTime

class CouponAdminV1Dto {
    data class CreateRequest(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long? = null,
        val expiredAt: ZonedDateTime,
    )

    data class UpdateRequest(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long? = null,
        val expiredAt: ZonedDateTime,
    )
}
