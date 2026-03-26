package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.GetCouponIssueStatusCriteria
import com.loopers.application.coupon.GetMyCouponsCriteria
import com.loopers.application.coupon.IssueCouponCriteria
import com.loopers.application.coupon.RequestCouponIssueCriteria
import com.loopers.application.coupon.UserGetCouponIssueStatusUseCase
import com.loopers.application.coupon.UserGetMyCouponsUseCase
import com.loopers.application.coupon.UserIssueCouponUseCase
import com.loopers.application.coupon.UserRequestCouponIssueUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponV1Controller(
    private val userIssueCouponUseCase: UserIssueCouponUseCase,
    private val userGetMyCouponsUseCase: UserGetMyCouponsUseCase,
    private val userRequestCouponIssueUseCase: UserRequestCouponIssueUseCase,
    private val userGetCouponIssueStatusUseCase: UserGetCouponIssueStatusUseCase,
) : CouponV1ApiSpec {

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    override fun issueCoupon(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse> {
        val criteria = IssueCouponCriteria(loginId = loginId, couponId = couponId)
        return userIssueCouponUseCase.execute(criteria)
            .let { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    override fun getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1Dto.IssuedCouponsResponse> {
        val criteria = GetMyCouponsCriteria(loginId = loginId, page = page, size = size)
        return userGetMyCouponsUseCase.execute(criteria)
            .let { CouponV1Dto.IssuedCouponsResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/{couponId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun issueAsyncCoupon(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueRequestResponse> {
        val criteria = RequestCouponIssueCriteria(loginId = loginId, couponId = couponId)
        return userRequestCouponIssueUseCase.execute(criteria)
            .let { CouponV1Dto.CouponIssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/issue-requests/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getCouponIssueStatus(
        @PathVariable requestId: Long,
    ): ApiResponse<CouponV1Dto.CouponIssueStatusResponse> {
        val criteria = GetCouponIssueStatusCriteria(requestId = requestId)
        return userGetCouponIssueStatusUseCase.execute(criteria)
            .let { CouponV1Dto.CouponIssueStatusResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
