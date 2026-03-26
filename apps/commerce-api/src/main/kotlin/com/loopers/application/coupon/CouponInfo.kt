package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.IssuedCoupon

class CouponInfo {

    data class Detail(
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
            fun from(coupon: Coupon) = Detail(
                id = requireNotNull(coupon.id) { "쿠폰 저장 후 ID가 할당되지 않았습니다." },
                name = coupon.name.value,
                type = coupon.type.name,
                discountValue = coupon.discountValue.value,
                minOrderAmount = coupon.minOrderAmount.value,
                expiredAt = coupon.expiredAt.toString(),
                issueLimit = coupon.issueLimit.value,
                issuedCount = coupon.issuedCount,
            )
        }
    }

    data class Main(
        val id: Long,
        val name: String,
        val type: String,
        val discountValue: Long,
        val expiredAt: String,
        val issueLimit: Long?,
        val issuedCount: Long,
    ) {
        companion object {
            fun from(coupon: Coupon) = Main(
                id = requireNotNull(coupon.id) { "쿠폰 저장 후 ID가 할당되지 않았습니다." },
                name = coupon.name.value,
                type = coupon.type.name,
                discountValue = coupon.discountValue.value,
                expiredAt = coupon.expiredAt.toString(),
                issueLimit = coupon.issueLimit.value,
                issuedCount = coupon.issuedCount,
            )
        }
    }

    data class IssuedDetail(
        val id: Long,
        val couponName: String,
        val couponType: String,
        val discountValue: Long,
        val status: String,
        val issuedAt: String,
        val expiredAt: String,
    ) {
        companion object {
            fun from(issuedCoupon: IssuedCoupon, coupon: Coupon) = IssuedDetail(
                id = requireNotNull(issuedCoupon.id) { "발급 쿠폰 저장 후 ID가 할당되지 않았습니다." },
                couponName = coupon.name.value,
                couponType = coupon.type.name,
                discountValue = coupon.discountValue.value,
                status = issuedCoupon.status.name,
                issuedAt = issuedCoupon.issuedAt.toString(),
                expiredAt = coupon.expiredAt.toString(),
            )
        }
    }

    data class IssuedMain(
        val id: Long,
        val couponId: Long,
        val memberId: Long,
        val status: String,
        val issuedAt: String,
    ) {
        companion object {
            fun from(issuedCoupon: IssuedCoupon) = IssuedMain(
                id = requireNotNull(issuedCoupon.id) { "발급 쿠폰 저장 후 ID가 할당되지 않았습니다." },
                couponId = issuedCoupon.couponId,
                memberId = issuedCoupon.memberId,
                status = issuedCoupon.status.name,
                issuedAt = issuedCoupon.issuedAt.toString(),
            )
        }
    }

    data class IssueRequestDetail(
        val requestId: Long,
        val couponId: Long,
        val memberId: Long,
        val status: String,
        val issuedCouponId: Long?,
        val failureReason: String?,
        val requestedAt: String,
    ) {
        companion object {
            fun from(request: com.loopers.domain.coupon.CouponIssueRequest) = IssueRequestDetail(
                requestId = requireNotNull(request.id) { "쿠폰 발급 요청 저장 후 ID가 할당되지 않았습니다." },
                couponId = request.couponId,
                memberId = request.memberId,
                status = request.status.name,
                issuedCouponId = request.issuedCouponId,
                failureReason = request.failureReason,
                requestedAt = request.requestedAt.toString(),
            )
        }
    }
}
