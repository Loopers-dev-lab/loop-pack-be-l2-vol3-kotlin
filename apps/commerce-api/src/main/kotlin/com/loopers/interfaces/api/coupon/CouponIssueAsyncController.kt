package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueFacade
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons")
class CouponIssueAsyncController(
    private val couponIssueFacade: CouponIssueFacade,
) : CouponIssueAsyncApiSpec {

    @PostMapping("/{couponId}/issue-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun issueAsync(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponIssueAsyncDto.IssueAsyncResponse> {
        val requestId = couponIssueFacade.issueAsync(couponId, userInfo.id)
        return ApiResponse.success(
            CouponIssueAsyncDto.IssueAsyncResponse(
                requestId = requestId,
                status = CouponIssueStatus.PENDING,
            ),
        )
    }

    @GetMapping("/issue-requests/{requestId}")
    override fun getIssueRequest(
        @PathVariable requestId: String,
    ): ApiResponse<CouponIssueAsyncDto.IssueRequestResponse> {
        val info = couponIssueFacade.getIssueRequest(requestId)
        return ApiResponse.success(CouponIssueAsyncDto.IssueRequestResponse.from(info))
    }
}
