package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {
    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    override fun issueCoupon(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse> {
        return couponFacade.issueCoupon(loginId, password, couponId)
            .let { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/{couponId}/issue-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestCouponIssue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueRequestResponse> {
        return couponFacade.requestCouponIssue(loginId, password, couponId)
            .let { CouponV1Dto.CouponIssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/issue-requests/{requestId}/status")
    fun getCouponIssueStatus(
        @PathVariable requestId: String,
    ): ApiResponse<CouponV1Dto.CouponIssueRequestResponse> {
        return couponFacade.getCouponIssueStatus(requestId)
            .let { CouponV1Dto.CouponIssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> {
        return couponFacade.getUserCoupons(loginId, password)
            .map { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
