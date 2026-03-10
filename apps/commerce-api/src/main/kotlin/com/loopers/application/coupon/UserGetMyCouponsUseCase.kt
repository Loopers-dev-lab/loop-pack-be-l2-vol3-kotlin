package com.loopers.application.coupon

import com.loopers.application.UseCase
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.user.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class UserGetMyCouponsUseCase(
    private val couponService: CouponService,
    private val userService: UserService,
) : UseCase<GetMyCouponsCriteria, UserListCouponsResult> {
    override fun execute(criteria: GetMyCouponsCriteria): UserListCouponsResult {
        val user = userService.getUser(criteria.loginId)
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = couponService.getUserCoupons(user.id, pageable)
        return UserListCouponsResult(
            content = slice.content.map { UserIssuedCouponResult.from(it) },
            page = slice.number,
            size = slice.size,
            hasNext = slice.hasNext(),
        )
    }
}
