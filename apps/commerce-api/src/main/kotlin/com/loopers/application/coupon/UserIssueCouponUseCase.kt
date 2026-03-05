package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.IssueCouponCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserIssueCouponUseCase(
    private val couponService: CouponService,
    private val userService: UserService,
) : UseCase<IssueCouponCriteria, UserIssuedCouponResult> {
    override fun execute(criteria: IssueCouponCriteria): UserIssuedCouponResult {
        val user = userService.getUser(criteria.loginId)
        val command = IssueCouponCommand(
            couponId = criteria.couponId,
            userId = user.id,
        )
        val info = couponService.issueCoupon(command)
        return UserIssuedCouponResult.from(info)
    }
}
