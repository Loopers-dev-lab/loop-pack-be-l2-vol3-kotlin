package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueResult
import com.loopers.application.coupon.CouponResult
import com.loopers.application.coupon.CreateCouponCriteria
import com.loopers.application.coupon.UpdateCouponCriteria
import java.time.ZonedDateTime

class CouponAdminV1Dto {

    data class CreateCouponRequest(
        val name: String,
        val type: String,
        val value: Long,
        val expiredAt: ZonedDateTime,
    ) {
        fun toCriteria(): CreateCouponCriteria {
            return CreateCouponCriteria(
                name = name,
                type = type,
                value = value,
                expiredAt = expiredAt,
            )
        }
    }

    data class UpdateCouponRequest(
        val name: String,
        val type: String,
        val value: Long,
        val expiredAt: ZonedDateTime,
    ) {
        fun toCriteria(): UpdateCouponCriteria {
            return UpdateCouponCriteria(
                name = name,
                type = type,
                value = value,
                expiredAt = expiredAt,
            )
        }
    }

    data class CouponAdminResponse(
        val id: Long,
        val name: String,
        val type: String,
        val value: Long,
        val expiredAt: ZonedDateTime,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: CouponResult): CouponAdminResponse {
                return CouponAdminResponse(
                    id = result.id,
                    name = result.name,
                    type = result.type,
                    value = result.value,
                    expiredAt = result.expiredAt,
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class CouponIssueAdminResponse(
        val id: Long,
        val couponId: Long,
        val userId: Long,
        val status: String,
        val usedAt: ZonedDateTime?,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: CouponIssueResult): CouponIssueAdminResponse {
                return CouponIssueAdminResponse(
                    id = result.id,
                    couponId = result.couponId,
                    userId = result.userId,
                    status = result.status,
                    usedAt = result.usedAt,
                    createdAt = result.createdAt,
                )
            }
        }
    }
}
