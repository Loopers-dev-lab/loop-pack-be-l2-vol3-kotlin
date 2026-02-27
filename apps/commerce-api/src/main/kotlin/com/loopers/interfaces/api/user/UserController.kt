package com.loopers.interfaces.api.user

import com.loopers.application.user.AuthenticatedUserInfo
import com.loopers.application.user.UserFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userFacade: UserFacade,
) : UserApiSpec {

    @PostMapping("/signup")
    override fun signUp(
        @RequestBody @Valid request: UserDto.SignUpRequest,
    ): ApiResponse<UserDto.SignUpResponse> {
        return userFacade.signUp(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            email = request.email,
            birthday = request.birthday,
        )
            .let { UserDto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMe(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
    ): ApiResponse<UserDto.MeResponse> {
        return userFacade.getMe(userInfo)
            .let { UserDto.MeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @RequestBody @Valid request: UserDto.ChangePasswordRequest,
    ): ApiResponse<Unit> {
        userFacade.changePassword(userInfo.id, request.currentPassword, request.newPassword)
        return ApiResponse.success(Unit)
    }
}
