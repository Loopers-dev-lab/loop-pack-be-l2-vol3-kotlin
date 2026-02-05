package com.loopers.interfaces.api.user

import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AuthHeader
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userService: UserService,
) : UserV1ApiSpec {

    @PostMapping("/signup")
    override fun signup(@RequestBody request: UserV1Dto.SignupRequest): ApiResponse<UserV1Dto.UserResponse> {
        return userService.createUser(
            userId = request.userId,
            password = request.password,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMyInfo(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
    ): ApiResponse<UserV1Dto.UserResponse> {
        val authHeader = AuthHeader(loginId, loginPw)
        return userService.authenticate(authHeader.loginId, authHeader.password)
            .let { UserV1Dto.UserResponse.fromMasked(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @RequestBody request: UserV1Dto.UserChangePasswordRequest,
    ): ApiResponse<Any> {
        val authHeader = AuthHeader(loginId, loginPw)
        userService.authenticate(authHeader.loginId, authHeader.password)
        userService.changePassword(authHeader.loginId, request.oldPassword, request.newPassword)
        return ApiResponse.success()
    }
}
