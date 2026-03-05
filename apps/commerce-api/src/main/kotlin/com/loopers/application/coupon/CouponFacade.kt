package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
    private val couponTransactionExecutor: CouponTransactionExecutor,
) {

    fun issue(couponId: Long, userId: Long) {
        try {
            couponTransactionExecutor.issue(couponId, userId)
        } catch (_: DataIntegrityViolationException) {
            // TOCTOU 경쟁 조건: 다른 스레드가 먼저 쿠폰을 발급함 → CONFLICT 예외로 변환
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getMyCoupons(userId: Long): List<MyCouponInfo> {
        val issuedCoupons = couponService.findIssuedCouponsByUserId(userId)
        if (issuedCoupons.isEmpty()) return emptyList()

        val couponIds = issuedCoupons.map { it.couponId }
        val couponMap = couponService.findCouponsByIds(couponIds).associateBy { it.id }

        return issuedCoupons.mapNotNull { issued ->
            couponMap[issued.couponId]?.let { coupon ->
                MyCouponInfo.from(issued, coupon)
            }
        }
    }
}
