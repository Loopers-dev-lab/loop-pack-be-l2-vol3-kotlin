package com.loopers.user.interfaces.api

import com.loopers.support.api.ApiResponse
import com.loopers.user.application.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    private val userService: UserService,
) : UserV1ApiSpec {
    @PostMapping
    override fun signUp(
        @Valid @RequestBody request: UserV1Dto.SignUpRequest,
    ): ResponseEntity<ApiResponse<UserV1Dto.SignUpResponse>> {
        return userService.signUp(request.toCommand())
            .let { UserV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ResponseEntity<ApiResponse<UserV1Dto.ChangePasswordResponse>> {
        userService.changePassword(loginId, password, request.toCommand())
        return UserV1Dto.ChangePasswordResponse.success()
            .let { ApiResponse.success(it) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/me")
    override fun getMe(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ResponseEntity<ApiResponse<UserV1Dto.MeResponse>> {
        return userService.getMe(loginId, password)
            .let { UserV1Dto.MeResponse.from(it) }
            .let { ApiResponse.success(it) }
            .let { ResponseEntity.ok(it) }
    }
}
