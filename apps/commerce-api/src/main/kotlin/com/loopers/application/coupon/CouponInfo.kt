package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import java.time.ZonedDateTime

data class CouponInfo(
    val id: Long,
    val name: String,
    val discountType: String,
    val discountValue: Long,
    val minOrderAmount: Long,
    val maxIssueCount: Int?,
    val issuedCount: Int,
    val expiredAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(coupon: Coupon): CouponInfo {
            val id = requireNotNull(coupon.persistenceId) {
                "Coupon.persistenceId가 null입니다. 저장된 Coupon만 매핑 가능합니다."
            }
            return CouponInfo(
                id = id,
                name = coupon.name.value,
                discountType = coupon.discountType.name,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount.amount,
                maxIssueCount = coupon.maxIssueCount,
                issuedCount = coupon.issuedCount,
                expiredAt = coupon.expiredAt,
                deletedAt = coupon.deletedAt,
            )
        }
    }
}
