package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.admin.coupon.AdminCouponResult
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminCouponV1Response {
    data class Register(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminCouponResult.Register): Register = Register(
                id = result.id,
                name = result.name,
                type = result.type,
                discountValue = result.discountValue,
                minOrderAmount = result.minOrderAmount,
                expiredAt = result.expiredAt,
            )
        }
    }

    data class Update(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminCouponResult.Update): Update = Update(
                id = result.id,
                name = result.name,
                type = result.type,
                discountValue = result.discountValue,
                minOrderAmount = result.minOrderAmount,
                expiredAt = result.expiredAt,
            )
        }
    }

    data class Detail(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminCouponResult.Detail): Detail = Detail(
                id = result.id,
                name = result.name,
                type = result.type,
                discountValue = result.discountValue,
                minOrderAmount = result.minOrderAmount,
                expiredAt = result.expiredAt,
                createdAt = result.createdAt,
            )
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: AdminCouponResult.Summary): Summary = Summary(
                id = result.id,
                name = result.name,
                type = result.type,
                discountValue = result.discountValue,
                expiredAt = result.expiredAt,
            )
        }
    }

    data class IssuedCouponItem(
        val id: Long,
        val couponId: Long,
        val userId: Long,
        val displayStatus: String,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(result: AdminCouponResult.IssuedCouponItem): IssuedCouponItem =
                IssuedCouponItem(
                    id = result.id,
                    couponId = result.couponId,
                    userId = result.userId,
                    displayStatus = result.displayStatus,
                    expiredAt = result.expiredAt,
                    usedAt = result.usedAt,
                )
        }
    }
}
