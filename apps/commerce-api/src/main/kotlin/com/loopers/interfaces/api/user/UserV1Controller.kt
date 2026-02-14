package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
) : UserV1ApiSpec {

    @PostMapping("/sign-up")
    override fun signUp(
        @RequestBody @Valid request: UserV1Dto.SignUpRequest,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.signUp(request.toCommand())
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/user")
    override fun getUserInfo(
        @AuthUser userId: Long,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.getUserInfo(userId)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/user/password")
    override fun changePassword(
        @AuthUser userId: Long,
        @RequestBody @Valid request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        userFacade.changePassword(userId, request.toCommand())
        return ApiResponse.success()
    }
}
