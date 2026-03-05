package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.GetCouponResult
import com.loopers.application.coupon.GetIssuedCouponResult
import com.loopers.application.coupon.ListCouponsResult
import com.loopers.application.coupon.ListIssuedCouponsResult
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import java.time.ZonedDateTime

class CouponV1AdminDto {
    data class RegisterRequest(
        val name: String,
        val discountType: DiscountType,
        val discountValue: Int,
        val totalQuantity: Int,
        val expiredAt: ZonedDateTime,
    )

    data class ModifyRequest(
        val name: String,
        val discountType: DiscountType,
        val discountValue: Int,
        val totalQuantity: Int,
        val expiredAt: ZonedDateTime,
    )

    data class CouponTemplateResponse(
        val id: Long,
        val name: String,
        val discountType: DiscountType,
        val discountValue: Int,
        val totalQuantity: Int,
        val issuedQuantity: Int,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: GetCouponResult): CouponTemplateResponse {
                return CouponTemplateResponse(
                    id = result.id,
                    name = result.name,
                    discountType = result.discountType,
                    discountValue = result.discountValue,
                    totalQuantity = result.totalQuantity,
                    issuedQuantity = result.issuedQuantity,
                    expiredAt = result.expiredAt,
                )
            }
        }
    }

    data class CouponTemplatesResponse(
        val content: List<CouponTemplateResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: ListCouponsResult): CouponTemplatesResponse {
                return CouponTemplatesResponse(
                    content = result.content.map { CouponTemplateResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }

    data class CouponTemplateDetailResponse(
        val id: Long,
        val name: String,
        val discountType: DiscountType,
        val discountValue: Int,
        val totalQuantity: Int,
        val issuedQuantity: Int,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: GetCouponResult): CouponTemplateDetailResponse {
                return CouponTemplateDetailResponse(
                    id = result.id,
                    name = result.name,
                    discountType = result.discountType,
                    discountValue = result.discountValue,
                    totalQuantity = result.totalQuantity,
                    issuedQuantity = result.issuedQuantity,
                    expiredAt = result.expiredAt,
                )
            }
        }
    }

    data class IssuedCouponResponse(
        val id: Long,
        val couponId: Long,
        val userId: Long,
        val status: CouponStatus,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(result: GetIssuedCouponResult): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = result.id,
                    couponId = result.couponId,
                    userId = result.userId,
                    status = result.status,
                    usedAt = result.usedAt,
                )
            }
        }
    }

    data class IssuedCouponsResponse(
        val content: List<IssuedCouponResponse>,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    ) {
        companion object {
            fun from(result: ListIssuedCouponsResult): IssuedCouponsResponse {
                return IssuedCouponsResponse(
                    content = result.content.map { IssuedCouponResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    hasNext = result.hasNext,
                )
            }
        }
    }
}
