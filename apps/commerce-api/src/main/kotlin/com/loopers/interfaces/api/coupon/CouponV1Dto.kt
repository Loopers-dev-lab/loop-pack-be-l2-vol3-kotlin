package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.domain.coupon.IssuedCouponStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

class CouponV1Dto {

    @Schema(description = "발급된 쿠폰 응답 (대고객)")
    data class IssuedCouponResponse(
        @Schema(description = "발급 쿠폰 ID", example = "1")
        val id: Long,
        @Schema(description = "쿠폰 템플릿 ID", example = "1")
        val couponId: Long,
        @Schema(description = "유저 ID", example = "1")
        val userId: Long,
        @Schema(description = "쿠폰 상태", example = "AVAILABLE")
        val status: IssuedCouponStatus,
        @Schema(description = "사용일시")
        val usedAt: ZonedDateTime?,
        @Schema(description = "발급일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    couponId = info.couponId,
                    userId = info.userId,
                    status = info.status,
                    usedAt = info.usedAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }
}
