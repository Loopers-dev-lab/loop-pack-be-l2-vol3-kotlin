package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
) : UserV1ApiSpec {

    @PostMapping
    override fun signUp(
        @RequestBody request: UserV1Dto.SignUpRequest,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.signUp(request.toCommand())
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMe(
        @RequestHeader(HttpHeaders.LOGIN_ID) loginId: String,
        @RequestHeader(HttpHeaders.LOGIN_PW) loginPw: String,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.getMe(loginId, loginPw)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader(HttpHeaders.LOGIN_ID) loginId: String,
        @RequestHeader(HttpHeaders.LOGIN_PW) loginPw: String,
        @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Unit> {
        userFacade.changePassword(loginId, loginPw, request.toCommand())
        return ApiResponse.success(Unit)
    }
}
