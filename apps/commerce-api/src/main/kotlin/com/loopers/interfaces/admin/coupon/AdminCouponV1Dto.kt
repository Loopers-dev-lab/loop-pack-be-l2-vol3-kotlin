package com.loopers.interfaces.admin.coupon

import com.loopers.domain.coupon.CouponType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.ZonedDateTime

object AdminCouponV1Dto {

    data class CreateCouponTemplateRequest(
        @NotBlank(message = "쿠폰 이름은 필수입니다")
        val name: String,

        @NotNull(message = "쿠폰 타입은 필수입니다")
        val type: CouponType,

        @NotNull(message = "할인액은 필수입니다")
        @PositiveOrZero(message = "할인액은 0 이상이어야 합니다")
        val value: BigDecimal,

        @NotNull(message = "최소 주문액은 필수입니다")
        val minOrderAmount: BigDecimal,

        @NotNull(message = "유효기간은 필수입니다")
        val expiredAt: ZonedDateTime,
    )

    data class UpdateCouponTemplateRequest(
        @NotBlank(message = "쿠폰 이름은 필수입니다")
        val name: String,

        @NotNull(message = "할인액은 필수입니다")
        @PositiveOrZero(message = "할인액은 0 이상이어야 합니다")
        val value: BigDecimal,

        @NotNull(message = "최소 주문액은 필수입니다")
        val minOrderAmount: BigDecimal,

        @NotNull(message = "유효기간은 필수입니다")
        val expiredAt: ZonedDateTime,
    )
}
