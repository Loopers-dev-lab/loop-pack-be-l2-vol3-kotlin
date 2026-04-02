package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponCommand
import com.loopers.application.coupon.GetCouponIssueResultUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RequestCouponIssueAsyncUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.constant.ApiPaths
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Coupons.BASE)
class CouponV1Controller(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val requestCouponIssueAsyncUseCase: RequestCouponIssueAsyncUseCase,
    private val getCouponIssueResultUseCase: GetCouponIssueResultUseCase,
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

    @PostMapping("/{couponId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun issueAsync(
        @CurrentUserId userId: Long,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponIssueRequestResponse> {
        val result = requestCouponIssueAsyncUseCase.execute(
            CouponCommand.IssueAsync(couponId = couponId, userId = userId),
        )
        return ApiResponse.success(CouponIssueRequestResponse.from(result))
    }

    @GetMapping("/issue-requests/{requestId}")
    fun getIssueResult(
        @PathVariable requestId: String,
    ): ApiResponse<CouponIssueRequestResponse> {
        val result = getCouponIssueResultUseCase.execute(requestId)
        return ApiResponse.success(CouponIssueRequestResponse.from(result))
    }
}
