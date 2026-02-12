package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.auth.RequireAuth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserV1Controller(
    private val userFacade: UserFacade,
) : UserV1ApiSpec {

    @PostMapping("/register")
    override fun register(
        @RequestBody request: UserV1Dto.UserRegisterRequest,
    ): ApiResponse<UserV1Dto.UserRegisterResponse> {
        return userFacade.register(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        ).let { UserV1Dto.UserRegisterResponse.from(it) }
         .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getUserInfo(
        @RequireAuth authenticatedUser: AuthenticatedUser,
    ): ApiResponse<UserV1Dto.UserInfoResponse> {
        return userFacade.getUserInfo(authenticatedUser.loginId)
            .let { UserV1Dto.UserInfoResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/password")
    override fun changePassword(
        @RequireAuth authenticatedUser: AuthenticatedUser,
        @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Unit> {
        userFacade.changePassword(
            loginId = authenticatedUser.loginId,
            birthDate = authenticatedUser.birthDate,
            newPassword = request.newPassword,
        )
        return ApiResponse.success(Unit)
    }
}
