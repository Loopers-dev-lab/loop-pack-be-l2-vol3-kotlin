package com.loopers.interfaces.api.user.coupon

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.coupon.UserCouponCommand
import com.loopers.application.user.coupon.UserCouponIssueUseCase
import com.loopers.application.user.coupon.UserCouponListUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserCouponV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val issueUseCase: UserCouponIssueUseCase,
    private val listUseCase: UserCouponListUseCase,
) : UserCouponV1ApiSpec {

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable couponId: Long,
    ): ApiResponse<UserCouponV1Response.Issued> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return issueUseCase.issue(UserCouponCommand.Issue(userId = userId, couponId = couponId))
            .let { UserCouponV1Response.Issued.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getList(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<UserCouponV1Response.ListItem>> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        return listUseCase.getList(userId)
            .map { UserCouponV1Response.ListItem.from(it) }
            .let { ApiResponse.success(it) }
    }
}
