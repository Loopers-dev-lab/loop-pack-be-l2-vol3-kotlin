package com.loopers.domain.coupon.dto

import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CouponTemplateDetailInfo(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: BigDecimal,
    val minOrderAmount: BigDecimal,
    val expiredAt: ZonedDateTime,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val isActive: Boolean,
) {
    companion object {
        fun from(template: CouponTemplate): CouponTemplateDetailInfo {
            return CouponTemplateDetailInfo(
                id = template.id,
                name = template.name,
                type = template.type,
                value = template.value,
                minOrderAmount = template.minOrderAmount,
                expiredAt = template.expiredAt,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt,
                isActive = template.deletedAt == null,
            )
        }
    }
}
