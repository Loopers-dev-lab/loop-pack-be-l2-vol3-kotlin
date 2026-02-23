package com.loopers.interfaces.api.user

import com.loopers.application.user.ChangePasswordUseCase
import com.loopers.application.user.GetUserInfoUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.interfaces.api.user.dto.UserV1Dto
import com.loopers.interfaces.api.user.spec.UserV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
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
    private val registerUserUseCase: RegisterUserUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) : UserV1ApiSpec {

    @PostMapping("/sign-up")
    override fun signUp(
        @RequestBody @Valid request: UserV1Dto.SignUpRequest,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return registerUserUseCase.execute(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/user")
    override fun getUserInfo(
        @AuthUser userId: Long,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return getUserInfoUseCase.execute(userId)
            .let { UserV1Dto.UserResponse.fromMasked(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/user/password")
    override fun changePassword(
        @AuthUser userId: Long,
        @RequestBody @Valid request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        changePasswordUseCase.execute(userId, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
