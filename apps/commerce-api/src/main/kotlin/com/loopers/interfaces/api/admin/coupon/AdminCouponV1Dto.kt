package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponTemplateInfo
import com.loopers.application.coupon.IssuedCouponInfo
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.ZonedDateTime

class AdminCouponV1Dto {
    data class CreateRequest(
        @field:NotBlank(message = "쿠폰명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "쿠폰 타입은 필수입니다.")
        val type: String,
        @field:Min(value = 1, message = "쿠폰 값은 1 이상이어야 합니다.")
        val value: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        @field:NotBlank(message = "만료 정책은 필수입니다.")
        val expirationPolicy: String,
        val expiredAt: ZonedDateTime?,
        val validDays: Int?,
    )

    data class UpdateRequest(
        @field:NotBlank(message = "쿠폰명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "쿠폰 타입은 필수입니다.")
        val type: String,
        @field:Min(value = 1, message = "쿠폰 값은 1 이상이어야 합니다.")
        val value: Long,
        val minOrderAmount: Long?,
        val maxDiscountAmount: Long?,
        @field:NotBlank(message = "만료 정책은 필수입니다.")
        val expirationPolicy: String,
        val expiredAt: ZonedDateTime?,
        val validDays: Int?,
    )

    data class TemplateResponse(
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
            fun from(info: CouponTemplateInfo): TemplateResponse {
                return TemplateResponse(
                    id = info.id,
                    name = info.name,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    maxDiscountAmount = info.maxDiscountAmount,
                    expirationPolicy = info.expirationPolicy,
                    expiredAt = info.expiredAt,
                    validDays = info.validDays,
                    status = info.status,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }

    data class IssuedCouponResponse(
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
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    couponTemplateId = info.couponTemplateId,
                    memberId = info.memberId,
                    status = info.status,
                    templateName = info.templateName,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    maxDiscountAmount = info.maxDiscountAmount,
                    expiredAt = info.expiredAt,
                    usedAt = info.usedAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
