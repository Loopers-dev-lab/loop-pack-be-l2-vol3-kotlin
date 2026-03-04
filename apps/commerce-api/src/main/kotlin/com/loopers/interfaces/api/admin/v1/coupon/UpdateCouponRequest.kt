package com.loopers.interfaces.api.admin.v1.coupon

import com.loopers.application.coupon.UpdateCouponCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.ZonedDateTime

data class UpdateCouponRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다.")
    val name: String,

    @field:NotBlank(message = "할인 유형은 필수입니다.")
    @field:Pattern(regexp = "FIXED|RATE", message = "할인 유형은 FIXED 또는 RATE이어야 합니다.")
    val discountType: String,

    @field:NotNull(message = "할인 값은 필수입니다.")
    @field:Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
    val discountValue: Long,

    @field:NotNull(message = "최소 주문 금액은 필수입니다.")
    @field:Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.")
    val minOrderAmount: Long,

    val maxIssueCount: Int?,

    @field:NotNull(message = "만료일은 필수입니다.")
    val expiredAt: ZonedDateTime,
) {
    fun toCommand() = UpdateCouponCommand(
        name = name,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        maxIssueCount = maxIssueCount,
        expiredAt = expiredAt,
    )
}
