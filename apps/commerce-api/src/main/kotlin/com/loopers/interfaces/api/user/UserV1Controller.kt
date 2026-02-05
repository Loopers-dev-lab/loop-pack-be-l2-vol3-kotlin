package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UpdatePasswordCommand
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userService: UserService,
) : UserV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestBody request: UserV1Dto.RegisterRequest,
    ) {
        userService.register(
            RegisterCommand(
                username = request.username,
                password = request.password,
                name = request.name,
                email = request.email,
                birthDate = request.birthDate,
            )
        )
    }

    @GetMapping("/me")
    override fun getMe(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
    ): ApiResponse<UserV1Dto.UserResponse> {
        userService.authenticate(loginId, loginPw)
        return userService.getUser(loginId)
            .let { UserInfo.from(it) }
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun updatePassword(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @RequestBody request: UserV1Dto.UpdatePasswordRequest,
    ) {
        userService.authenticate(loginId, loginPw)
        userService.updatePassword(
            UpdatePasswordCommand(
                username = loginId,
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
            )
        )
    }
}
