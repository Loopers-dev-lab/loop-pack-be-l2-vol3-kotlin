package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.admin.coupon.AdminCouponCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
import java.time.ZonedDateTime

class AdminCouponV1Request {
    data class Register(
        @field:NotBlank
        val name: String,
        @field:NotBlank
        @field:Pattern(regexp = "FIXED|RATE", message = "FIXED 또는 RATE만 허용됩니다.")
        val type: String,
        @field:NotNull
        @field:Min(1)
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        @field:NotNull
        val expiredAt: ZonedDateTime,
    ) {
        fun toCommand(admin: String): AdminCouponCommand.Register = AdminCouponCommand.Register(
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            admin = admin,
        )
    }

    data class Update(
        @field:NotBlank
        val name: String,
        @field:NotNull
        @field:Min(1)
        val discountValue: Long,
        val minOrderAmount: BigDecimal?,
        @field:NotNull
        val expiredAt: ZonedDateTime,
    ) {
        fun toCommand(couponId: Long, admin: String): AdminCouponCommand.Update =
            AdminCouponCommand.Update(
                couponId = couponId,
                name = name,
                discountValue = discountValue,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
                admin = admin,
            )
    }
}
