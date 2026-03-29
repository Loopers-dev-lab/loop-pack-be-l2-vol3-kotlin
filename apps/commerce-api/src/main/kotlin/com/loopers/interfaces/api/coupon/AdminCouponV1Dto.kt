package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponAdminUseCase
import com.loopers.application.coupon.CouponInfo
import com.loopers.domain.coupon.CouponType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.ZonedDateTime

class AdminCouponV1Dto {

    data class RegisterRequest(
        @field:NotBlank val name: String,
        @field:NotNull val type: CouponType,
        @field:NotNull val discountValue: Long,
        val minOrderAmount: Long?,
        val issueLimit: Long?,
        @field:NotNull val expiredAt: ZonedDateTime,
    ) {
        fun toCommand() = CouponAdminUseCase.RegisterCommand(
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            issueLimit = issueLimit,
        )
    }

    data class UpdateRequest(
        @field:NotBlank val name: String,
        @field:NotNull val type: CouponType,
        @field:NotNull val discountValue: Long,
        val minOrderAmount: Long?,
        val issueLimit: Long?,
        @field:NotNull val expiredAt: ZonedDateTime,
    ) {
        fun toCommand() = CouponAdminUseCase.UpdateCommand(
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            issueLimit = issueLimit,
        )
    }

    data class DetailResponse(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val minOrderAmount: Long?,
        val expiredAt: String,
        val issueLimit: Long?,
        val issuedCount: Long,
    ) {
        companion object {
            fun from(info: CouponInfo.Detail) = DetailResponse(
                id = info.id,
                name = info.name,
                type = info.type,
                discountValue = info.discountValue,
                minOrderAmount = info.minOrderAmount,
                expiredAt = info.expiredAt,
                issueLimit = info.issueLimit,
                issuedCount = info.issuedCount,
            )
        }
    }

    data class MainResponse(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val expiredAt: String,
        val issueLimit: Long?,
        val issuedCount: Long,
    ) {
        companion object {
            fun from(info: CouponInfo.Main) = MainResponse(
                id = info.id,
                name = info.name,
                type = info.type,
                discountValue = info.discountValue,
                expiredAt = info.expiredAt,
                issueLimit = info.issueLimit,
                issuedCount = info.issuedCount,
            )
        }
    }

    data class IssuedMainResponse(
        val id: Long,
        val couponId: Long,
        val memberId: Long,
        val status: String,
        val issuedAt: String,
    ) {
        companion object {
            fun from(info: CouponInfo.IssuedMain) = IssuedMainResponse(
                id = info.id,
                couponId = info.couponId,
                memberId = info.memberId,
                status = info.status,
                issuedAt = info.issuedAt,
            )
        }
    }
}
