package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponTemplateModel
import java.time.ZonedDateTime

data class CouponTemplateInfo(
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
        fun from(model: CouponTemplateModel) = CouponTemplateInfo(
            id = model.id,
            name = model.name.value,
            type = model.type.name,
            value = model.value.value,
            minOrderAmount = model.minOrderAmount?.value,
            expiredAt = model.expiredAt,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}
