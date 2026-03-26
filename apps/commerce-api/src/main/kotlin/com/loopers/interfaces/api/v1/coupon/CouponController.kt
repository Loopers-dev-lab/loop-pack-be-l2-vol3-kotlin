package com.loopers.interfaces.api.v1.coupon

import com.loopers.application.coupon.GetCouponIssueRequestUseCase
import com.loopers.application.coupon.GetMyCouponsUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RequestCouponIssueAsyncUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController(
    private val issueCouponUseCase: IssueCouponUseCase,
    private val getMyCouponsUseCase: GetMyCouponsUseCase,
    private val requestCouponIssueAsyncUseCase: RequestCouponIssueAsyncUseCase,
    private val getCouponIssueRequestUseCase: GetCouponIssueRequestUseCase,
) {
    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable couponId: Long,
    ): ApiResponse<IssueCouponResponse> {
        val id = issueCouponUseCase.issue(authUser.id, couponId)
        return ApiResponse.success(IssueCouponResponse(id))
    }

    @GetMapping("/me")
    fun getMyCoupons(
        @AuthenticatedUser authUser: AuthUser,
    ): ApiResponse<List<GetMyCouponResponse>> {
        val coupons = getMyCouponsUseCase.getMyAll(authUser.id)
        return ApiResponse.success(coupons.map { GetMyCouponResponse.from(it) })
    }

    @PostMapping("/{couponId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun issueAsync(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponIssueAsyncResponse> {
        val requestId = requestCouponIssueAsyncUseCase.request(authUser.id, couponId)
        return ApiResponse.success(CouponIssueAsyncResponse(requestId))
    }

    @GetMapping("/issue-requests/{requestId}")
    fun getIssueRequest(
        @PathVariable requestId: String,
    ): ApiResponse<CouponIssueRequestResponse> {
        val request = getCouponIssueRequestUseCase.getByRequestId(requestId)
        return ApiResponse.success(CouponIssueRequestResponse.from(request))
    }
}
