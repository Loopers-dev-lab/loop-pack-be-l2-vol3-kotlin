package com.loopers.interfaces.api.coupon.dto

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.CouponInfo
import com.loopers.application.coupon.IssuedCouponInfo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponAdminV1Dto {

    data class CreateCouponRequest(
        @field:NotBlank(message = "쿠폰명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "쿠폰 타입은 필수입니다.")
        val type: String,
        @field:Positive(message = "쿠폰 값은 양수여야 합니다.")
        val value: Long,
        @field:Positive(message = "최대 할인 금액은 양수여야 합니다.")
        val maxDiscount: BigDecimal? = null,
        @field:PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        val minOrderAmount: BigDecimal? = null,
        @field:Positive(message = "총 수량은 양수여야 합니다.")
        val totalQuantity: Int? = null,
        @field:NotBlank(message = "만료일은 필수입니다.")
        val expiredAt: String,
    ) {
        fun toCommand(): CouponCommand.CreateCoupon {
            return CouponCommand.CreateCoupon(
                name = name,
                type = type,
                value = value,
                maxDiscount = maxDiscount,
                minOrderAmount = minOrderAmount,
                totalQuantity = totalQuantity,
                expiredAt = ZonedDateTime.parse(expiredAt),
            )
        }
    }

    data class UpdateCouponRequest(
        val name: String? = null,
        val type: String? = null,
        @field:Positive(message = "쿠폰 값은 양수여야 합니다.")
        val value: Long? = null,
        @field:Positive(message = "최대 할인 금액은 양수여야 합니다.")
        val maxDiscount: BigDecimal? = null,
        @field:PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        val minOrderAmount: BigDecimal? = null,
        @field:Positive(message = "총 수량은 양수여야 합니다.")
        val totalQuantity: Int? = null,
        val expiredAt: String? = null,
    ) {
        fun toCommand(): CouponCommand.UpdateCoupon {
            return CouponCommand.UpdateCoupon(
                name = name,
                type = type,
                value = value,
                maxDiscount = maxDiscount,
                minOrderAmount = minOrderAmount,
                totalQuantity = totalQuantity,
                expiredAt = expiredAt?.let { ZonedDateTime.parse(it) },
            )
        }
    }

    data class CouponAdminResponse(
        val id: Long,
        val name: String,
        val type: String,
        val value: Long,
        val maxDiscount: BigDecimal?,
        val minOrderAmount: BigDecimal?,
        val totalQuantity: Int?,
        val issuedCount: Int,
        val expiredAt: String,
    ) {
        companion object {
            fun from(info: CouponInfo): CouponAdminResponse {
                return CouponAdminResponse(
                    id = info.id,
                    name = info.name,
                    type = info.type,
                    value = info.value,
                    maxDiscount = info.maxDiscount,
                    minOrderAmount = info.minOrderAmount,
                    totalQuantity = info.totalQuantity,
                    issuedCount = info.issuedCount,
                    expiredAt = info.expiredAt,
                )
            }
        }
    }

    data class IssuedCouponAdminResponse(
        val id: Long,
        val couponId: Long,
        val userId: Long,
        val status: String,
        val usedAt: String?,
        val createdAt: String,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponAdminResponse {
                return IssuedCouponAdminResponse(
                    id = info.id,
                    couponId = info.couponId,
                    userId = info.userId,
                    status = info.status,
                    usedAt = info.usedAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
