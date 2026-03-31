package com.loopers.application.handler.command.coupon

import com.loopers.application.coupon.CouponService
import com.loopers.domain.common.command.UseCouponCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class UseCouponCommandHandler(
    private val couponService: CouponService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: UseCouponCommand) {
        couponService.useCoupon(command.issuedCouponId, command.memberId)
    }
}
