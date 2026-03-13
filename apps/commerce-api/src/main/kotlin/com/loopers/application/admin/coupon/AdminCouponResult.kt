package com.loopers.application.admin.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.IssuedCoupon
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminCouponResult {
    data class Register(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(coupon: Coupon): Register = Register(
                id = coupon.id!!,
                name = coupon.name,
                type = coupon.type.name,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount?.amount,
                expiredAt = coupon.expiredAt,
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
            fun from(coupon: Coupon): Detail = Detail(
                id = coupon.id!!,
                name = coupon.name,
                type = coupon.type.name,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount?.amount,
                expiredAt = coupon.expiredAt,
                createdAt = coupon.createdAt!!,
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
            fun from(coupon: Coupon): Summary = Summary(
                id = coupon.id!!,
                name = coupon.name,
                type = coupon.type.name,
                discountValue = coupon.discountValue,
                expiredAt = coupon.expiredAt,
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
            fun from(coupon: Coupon): Update = Update(
                id = coupon.id!!,
                name = coupon.name,
                type = coupon.type.name,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount?.amount,
                expiredAt = coupon.expiredAt,
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
            fun from(issuedCoupon: IssuedCoupon): IssuedCouponItem = IssuedCouponItem(
                id = issuedCoupon.id!!,
                couponId = issuedCoupon.couponId,
                userId = issuedCoupon.userId,
                displayStatus = issuedCoupon.displayStatus().name,
                expiredAt = issuedCoupon.expiredAt,
                usedAt = issuedCoupon.usedAt,
            )
        }
    }
}
