package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCouponInfo
import java.time.ZonedDateTime

data class GetCouponResult(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Int,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(info: CouponInfo): GetCouponResult {
            return GetCouponResult(
                id = info.id,
                name = info.name,
                discountType = info.discountType,
                discountValue = info.discountValue,
                totalQuantity = info.totalQuantity,
                issuedQuantity = info.issuedQuantity,
                expiredAt = info.expiredAt,
            )
        }
    }
}

data class ListCouponsResult(
    val content: List<GetCouponResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)

data class GetIssuedCouponResult(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val discountType: DiscountType,
    val discountValue: Int,
    val status: CouponStatus,
    val expiredAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(info: IssuedCouponInfo): GetIssuedCouponResult {
            return GetIssuedCouponResult(
                id = info.id,
                couponId = info.couponId,
                userId = info.userId,
                discountType = info.discountType,
                discountValue = info.discountValue,
                status = info.status,
                expiredAt = info.expiredAt,
                usedAt = info.usedAt,
            )
        }
    }
}

data class ListIssuedCouponsResult(
    val content: List<GetIssuedCouponResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
)
