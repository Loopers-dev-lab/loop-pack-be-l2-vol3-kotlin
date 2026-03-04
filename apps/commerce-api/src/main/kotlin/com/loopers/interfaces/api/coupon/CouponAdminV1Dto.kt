package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponTemplateInfo
import com.loopers.application.coupon.UserCouponInfo
import java.time.ZonedDateTime

class CouponAdminV1Dto {

    data class CreateCouponTemplateRequest(
        val name: String,
        val type: String,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class UpdateCouponTemplateRequest(
        val name: String,
        val type: String,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    )

    data class CouponTemplateResponse(
        val id: Long,
        val name: String,
        val type: String,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponTemplateInfo) = CouponTemplateResponse(
                id = info.id,
                name = info.name,
                type = info.type,
                value = info.value,
                minOrderAmount = info.minOrderAmount,
                expiredAt = info.expiredAt,
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
            )
        }
    }

    data class UserCouponIssueResponse(
        val id: Long,
        val userId: Long,
        val couponTemplateId: Long,
        val status: String,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: UserCouponInfo) = UserCouponIssueResponse(
                id = info.id,
                userId = info.userId,
                couponTemplateId = info.couponTemplateId,
                status = info.status,
                createdAt = info.createdAt,
            )
        }
    }
}
