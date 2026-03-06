package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponFacade(
    private val couponService: CouponService,
) {

    @Transactional
    fun issue(couponId: Long, userId: Long) {
        couponService.issue(couponId, userId)
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
