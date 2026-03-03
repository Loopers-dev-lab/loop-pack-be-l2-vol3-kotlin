package com.loopers.interfaces.api.coupon.dto

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.CouponInfo
import com.loopers.application.coupon.IssuedCouponInfo
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

class CouponAdminV1Dto {

    data class CreateCouponRequest(
        @field:NotBlank(message = "쿠폰명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "쿠폰 타입은 필수입니다.")
        val type: String,
        val value: Long,
        val maxDiscount: BigDecimal? = null,
        val minOrderAmount: BigDecimal? = null,
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
                expiredAt = expiredAt,
            )
        }
    }

    data class UpdateCouponRequest(
        val name: String? = null,
        val type: String? = null,
        val value: Long? = null,
        val maxDiscount: BigDecimal? = null,
        val minOrderAmount: BigDecimal? = null,
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
                expiredAt = expiredAt,
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
