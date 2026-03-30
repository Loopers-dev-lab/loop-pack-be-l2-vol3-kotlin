package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.FcfsCouponService
import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponFcfsV1Controller(
    private val fcfsCouponService: FcfsCouponService,
    private val userService: UserService,
) {

    @PostMapping("/api/v1/coupons/{couponId}/fcfs-issue")
    fun requestFcfsIssue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ResponseEntity<ApiResponse<CouponV1Dto.FcfsIssueResponse>> {
        val authUser = userService.authenticate(loginId, password)
        val result = fcfsCouponService.requestFcfsIssue(couponId, authUser.id)
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(CouponV1Dto.FcfsIssueResponse(requestId = result.requestId)))
    }

    @GetMapping("/api/v1/coupons/fcfs-issue/status")
    fun getFcfsIssueStatus(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam requestId: String,
    ): ApiResponse<CouponV1Dto.FcfsStatusResponse> {
        val authUser = userService.authenticate(loginId, password)
        val result = fcfsCouponService.getIssueStatus(requestId, authUser.id)
        return ApiResponse.success(
            CouponV1Dto.FcfsStatusResponse(
                requestId = result.requestId,
                couponId = result.couponId,
                status = result.status,
                failureReason = result.failureReason,
            ),
        )
    }
}
