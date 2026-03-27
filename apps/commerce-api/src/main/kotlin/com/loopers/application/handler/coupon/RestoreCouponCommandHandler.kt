package com.loopers.application.handler.coupon

import com.loopers.application.coupon.CouponService
import com.loopers.domain.common.command.RestoreCouponCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RestoreCouponCommandHandler(
    private val couponService: CouponService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: RestoreCouponCommand) {
        couponService.restoreCoupon(command.issuedCouponId)
    }
}
