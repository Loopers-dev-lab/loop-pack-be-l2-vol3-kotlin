package com.loopers.domain.coupon.dto

import com.loopers.domain.coupon.CouponIssuanceResult
import com.loopers.domain.coupon.IssuanceStatus
import java.time.ZonedDateTime

data class CouponIssuanceStatusInfo(
    val dedupeKey: String,
    val status: IssuanceStatus,
    val couponId: Long? = null,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime? = null,
) {
    companion object {
        fun from(result: CouponIssuanceResult): CouponIssuanceStatusInfo {
            return CouponIssuanceStatusInfo(
                dedupeKey = result.dedupeKey,
                status = result.status,
                couponId = result.couponId,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
            )
        }
    }
}
