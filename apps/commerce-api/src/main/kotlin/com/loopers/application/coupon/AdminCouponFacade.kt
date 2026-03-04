package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.stereotype.Component

@Component
class AdminCouponFacade(
    private val couponService: CouponService,
) {

    fun getCoupons(pageQuery: PageQuery): PageResult<CouponInfo> {
        return couponService.findAll(pageQuery)
            .map { CouponInfo.from(it) }
    }

    fun getCoupon(couponId: Long): CouponInfo {
        return couponService.findCouponById(couponId)
            .let { CouponInfo.from(it) }
    }
}
