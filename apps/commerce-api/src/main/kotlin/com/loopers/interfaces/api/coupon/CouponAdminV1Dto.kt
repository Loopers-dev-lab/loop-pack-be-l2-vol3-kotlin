package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponInfo
import com.loopers.application.coupon.CreateCouponCriteria
import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.application.coupon.UpdateCouponCriteria
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCouponStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponAdminV1Dto {

    @Schema(description = "쿠폰 응답 (어드민)")
    data class CouponAdminResponse(
        @Schema(description = "쿠폰 ID", example = "1")
        val id: Long,
        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        val name: String,
        @Schema(description = "쿠폰 유형", example = "FIXED")
        val type: CouponType,
        @Schema(description = "할인 값", example = "5000")
        val value: BigDecimal,
        @Schema(description = "최소 주문 금액", example = "10000")
        val minOrderAmount: BigDecimal?,
        @Schema(description = "만료일시")
        val expiredAt: ZonedDateTime,
        @Schema(description = "생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponInfo): CouponAdminResponse {
                return CouponAdminResponse(
                    id = info.id,
                    name = info.name,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    expiredAt = info.expiredAt,
                    createdAt = info.createdAt,
                )
            }
        }
    }

    @Schema(description = "쿠폰 등록 요청")
    data class CreateRequest(
        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        val name: String,
        @Schema(description = "쿠폰 유형 (FIXED: 정액, RATE: 정률)", example = "FIXED")
        val type: CouponType,
        @Schema(description = "할인 값 (FIXED: 금액, RATE: 퍼센트)", example = "5000")
        val value: BigDecimal,
        @Schema(description = "최소 주문 금액", example = "10000")
        val minOrderAmount: BigDecimal?,
        @Schema(description = "만료일시", example = "2026-12-31T23:59:59+09:00")
        val expiredAt: ZonedDateTime,
    ) {
        fun toCriteria(): CreateCouponCriteria {
            return CreateCouponCriteria(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
        }
    }

    @Schema(description = "쿠폰 수정 요청")
    data class UpdateRequest(
        @Schema(description = "쿠폰명", example = "수정된 쿠폰")
        val name: String,
        @Schema(description = "할인 값", example = "3000")
        val value: BigDecimal,
        @Schema(description = "최소 주문 금액", example = "5000")
        val minOrderAmount: BigDecimal?,
        @Schema(description = "만료일시", example = "2026-12-31T23:59:59+09:00")
        val expiredAt: ZonedDateTime,
    ) {
        fun toCriteria(): UpdateCouponCriteria {
            return UpdateCouponCriteria(
                name = name,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
        }
    }

    @Schema(description = "발급된 쿠폰 응답 (어드민)")
    data class IssuedCouponAdminResponse(
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
            fun from(info: IssuedCouponInfo): IssuedCouponAdminResponse {
                return IssuedCouponAdminResponse(
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
