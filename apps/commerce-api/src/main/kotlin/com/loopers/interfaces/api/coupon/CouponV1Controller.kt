package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.GetMyCouponsUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.domain.coupon.UserCouponStatus
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Coupons.BASE)
class CouponV1Controller(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val getMyCouponsUseCase: GetMyCouponsUseCase,
) {

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(
        @CurrentUserId userId: Long,
        @PathVariable couponId: Long,
    ): ApiResponse<UserCouponResponse> {
        val result = issueCouponUseCase.execute(CouponCommand.Issue(couponId = couponId, userId = userId))
        return ApiResponse.success(UserCouponResponse.from(result))
    }

    @GetMapping("/me")
    fun getMyCoupons(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) status: UserCouponStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<UserCouponResponse>> {
        val result = getMyCouponsUseCase.execute(userId, status, page, size)
        val response = PageResult.of(
            content = result.content.map { UserCouponResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }
}
