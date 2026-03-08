package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val couponFacade: CouponFacade,
) : CouponApiSpec {

    @PostMapping("/{couponId}/issue")
    override fun issue(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable couponId: Long,
    ): ApiResponse<Unit> {
        couponFacade.issue(couponId, userInfo.id)
        return ApiResponse.success(Unit)
    }
}
