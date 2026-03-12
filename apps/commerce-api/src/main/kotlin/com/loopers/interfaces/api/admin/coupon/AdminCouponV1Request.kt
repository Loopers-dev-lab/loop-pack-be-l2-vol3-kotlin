package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponCommand
import com.loopers.domain.coupon.CouponType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

data class AdminCouponRegisterRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다.")
    @field:Size(min = 1, max = 50, message = "쿠폰명은 1~50자여야 합니다.")
    val name: String,

    @field:NotNull(message = "쿠폰 유형은 필수입니다.")
    val type: CouponType?,

    @field:NotNull(message = "할인 값은 필수입니다.")
    @field:Positive(message = "할인 값은 양수여야 합니다.")
    val value: Long?,

    val minOrderAmount: Long?,

    @field:NotNull(message = "만료일은 필수입니다.")
    val expiredAt: ZonedDateTime?,
) {
    fun toCommand(): CouponCommand.Register {
        return CouponCommand.Register(
            name = name.trim(),
            type = requireNotNull(type),
            value = requireNotNull(value),
            minOrderAmount = minOrderAmount,
            expiredAt = requireNotNull(expiredAt),
        )
    }
}

data class AdminCouponUpdateRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다.")
    @field:Size(min = 1, max = 50, message = "쿠폰명은 1~50자여야 합니다.")
    val name: String,

    @field:NotNull(message = "할인 값은 필수입니다.")
    @field:Positive(message = "할인 값은 양수여야 합니다.")
    val value: Long?,

    val minOrderAmount: Long?,

    @field:NotNull(message = "만료일은 필수입니다.")
    val expiredAt: ZonedDateTime?,
) {
    fun toCommand(couponId: Long): CouponCommand.Update {
        return CouponCommand.Update(
            couponId = couponId,
            name = name.trim(),
            value = requireNotNull(value),
            minOrderAmount = minOrderAmount,
            expiredAt = requireNotNull(expiredAt),
        )
    }
}
