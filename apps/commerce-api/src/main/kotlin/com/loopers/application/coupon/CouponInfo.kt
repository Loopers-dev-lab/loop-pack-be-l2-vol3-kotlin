package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import java.time.ZonedDateTime

data class CouponTemplateInfo(
    val id: Long,
    val name: String,
    val type: CouponType,
    val value: Long,
    val minOrderAmount: Long?,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(template: CouponTemplate): CouponTemplateInfo {
            return CouponTemplateInfo(
                id = template.id,
                name = template.name,
                type = template.type,
                value = template.value,
                minOrderAmount = template.minOrderAmount,
                expiredAt = template.expiredAt,
            )
        }
    }
}

data class IssuedCouponInfo(
    val id: Long,
    val couponTemplateId: Long,
    val couponName: String,
    val couponType: CouponType,
    val couponValue: Long,
    val minOrderAmount: Long?,
    val status: CouponStatus,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(issuedCoupon: IssuedCoupon, template: CouponTemplate): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = issuedCoupon.id,
                couponTemplateId = template.id,
                couponName = template.name,
                couponType = template.type,
                couponValue = template.value,
                minOrderAmount = template.minOrderAmount,
                status = issuedCoupon.getStatus(template.expiredAt),
                expiredAt = template.expiredAt,
            )
        }
    }
}
