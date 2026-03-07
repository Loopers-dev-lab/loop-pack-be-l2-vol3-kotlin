package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueResult
import com.loopers.application.coupon.MyCouponResult
import java.time.ZonedDateTime

class CouponV1Dto {

    data class CouponIssueResponse(
        val id: Long,
        val couponId: Long,
        val status: String,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: CouponIssueResult): CouponIssueResponse {
                return CouponIssueResponse(
                    id = result.id,
                    couponId = result.couponId,
                    status = result.status,
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class MyCouponResponse(
        val couponIssueId: Long,
        val couponId: Long,
        val name: String,
        val type: String,
        val value: Long,
        val status: String,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
        val issuedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: MyCouponResult): MyCouponResponse {
                return MyCouponResponse(
                    couponIssueId = result.couponIssueId,
                    couponId = result.couponId,
                    name = result.name,
                    type = result.type,
                    value = result.value,
                    status = result.status,
                    expiredAt = result.expiredAt,
                    usedAt = result.usedAt,
                    issuedAt = result.issuedAt,
                )
            }
        }
    }
}
