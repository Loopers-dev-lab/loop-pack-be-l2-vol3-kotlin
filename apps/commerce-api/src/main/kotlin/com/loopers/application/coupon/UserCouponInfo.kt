package com.loopers.application.coupon

import com.loopers.domain.coupon.UserCouponModel
import java.time.ZonedDateTime

data class UserCouponInfo(
    val id: Long,
    val userId: Long,
    val couponTemplateId: Long,
    val status: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(model: UserCouponModel) = UserCouponInfo(
            id = model.id,
            userId = model.userId,
            couponTemplateId = model.couponTemplateId,
            status = model.status.name,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}
