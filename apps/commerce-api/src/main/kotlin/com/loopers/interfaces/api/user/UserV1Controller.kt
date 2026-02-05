package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
)  : UserV1ApiSpec {
    @PostMapping("/register")
    override fun registerUser(@RequestBody req: UserV1Dto.RegisterUserRequest): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.registerUser(req.loginId, req.password, req.name, req.birth, req.email)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/info")
    override fun getUserInfo(
        @RequestHeader("X-Loopers-LoginId", required = true) loginId: String,
        @RequestHeader("X-Loopers-LoginPw", required = true) password: String,
        ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.getUser(loginId, password)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/changePassword")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId", required = true) loginId: String,
        @RequestHeader("X-Loopers-LoginPw", required = true) oldPassword: String,
        @RequestBody req: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.changePassword(loginId, oldPassword, req.newPassword)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
