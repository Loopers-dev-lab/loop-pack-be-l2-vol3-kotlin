package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssuance
import com.loopers.domain.coupon.CouponIssuanceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueService(
    private val couponIssuanceRepository: CouponIssuanceRepository,
) {

    @Transactional
    fun issue(couponId: Long, userId: Long): CouponIssueResult {
        val coupon = couponIssuanceRepository.findCouponById(couponId)
            ?: return CouponIssueResult.failed("쿠폰을 찾을 수 없습니다.")

        if (coupon.isDeleted()) {
            return CouponIssueResult.failed("삭제된 쿠폰입니다.")
        }

        if (coupon.isExpired()) {
            return CouponIssueResult.failed("만료된 쿠폰입니다.")
        }

        if (!coupon.isFcfsCoupon()) {
            return CouponIssueResult.failed("선착순 쿠폰이 아닙니다.")
        }

        if (couponIssuanceRepository.existsByCouponIdAndUserId(couponId, userId)) {
            return CouponIssueResult.failed("이미 발급받은 쿠폰입니다.")
        }

        val issuedCount = couponIssuanceRepository.countByCouponId(couponId)
        if (issuedCount >= coupon.maxIssueCount!!) {
            return CouponIssueResult.failed("수량이 소진되었습니다.")
        }

        couponIssuanceRepository.save(CouponIssuance(couponId = couponId, userId = userId))
        return CouponIssueResult.success()
    }
}

data class CouponIssueResult(
    val success: Boolean,
    val failureReason: String?,
) {
    companion object {
        fun success() = CouponIssueResult(success = true, failureReason = null)
        fun failed(reason: String) = CouponIssueResult(success = false, failureReason = reason)
    }
}
