package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.IssuedCouponModel
import java.time.ZonedDateTime

data class CouponTemplateInfo(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val minOrderAmount: Long?,
    val maxDiscountAmount: Long?,
    val expirationPolicy: String,
    val expiredAt: ZonedDateTime?,
    val validDays: Int?,
    val status: String,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(model: CouponTemplateModel): CouponTemplateInfo {
            return CouponTemplateInfo(
                id = model.id,
                name = model.name,
                type = model.type.name,
                value = model.value,
                minOrderAmount = model.minOrderAmount,
                maxDiscountAmount = model.maxDiscountAmount,
                expirationPolicy = model.expirationPolicy.name,
                expiredAt = model.expiredAt,
                validDays = model.validDays,
                status = model.status.name,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
            )
        }
    }
}

data class IssuedCouponInfo(
    val id: Long,
    val couponTemplateId: Long,
    val memberId: Long,
    val status: String,
    val templateName: String,
    val type: String,
    val value: Long,
    val minOrderAmount: Long?,
    val maxDiscountAmount: Long?,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(issued: IssuedCouponModel, template: CouponTemplateModel): IssuedCouponInfo {
            val status = when {
                issued.status == CouponStatus.USED -> "USED"
                issued.isExpired() -> "EXPIRED"
                else -> "AVAILABLE"
            }
            return IssuedCouponInfo(
                id = issued.id,
                couponTemplateId = issued.couponTemplateId,
                memberId = issued.memberId,
                status = status,
                templateName = template.name,
                type = template.type.name,
                value = template.value,
                minOrderAmount = template.minOrderAmount,
                maxDiscountAmount = template.maxDiscountAmount,
                expiredAt = issued.expiredAt,
                usedAt = issued.usedAt,
                createdAt = issued.createdAt,
            )
        }
    }
}
