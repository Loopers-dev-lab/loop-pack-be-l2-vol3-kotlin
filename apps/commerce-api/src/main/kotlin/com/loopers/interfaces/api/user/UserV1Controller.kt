package com.loopers.interfaces.api.user

import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userService: UserService,
) : UserV1ApiSpec {

    @PostMapping
    override fun signUp(
        @RequestBody request: UserV1Dto.SignUpRequest,
    ): ApiResponse<Any> {
        userService.signUp(request.toCriteria())
        return ApiResponse.success()
    }

    @GetMapping("/me")
    override fun getMyInfo(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<UserV1Dto.UserInfoResponse> {
        val authUser = userService.authenticate(loginId, password)
        val userInfo = userService.getMyInfo(authUser.id)
        return UserV1Dto.UserInfoResponse.from(userInfo)
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        val authUser = userService.authenticate(loginId, password)
        userService.changePassword(authUser.id, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
