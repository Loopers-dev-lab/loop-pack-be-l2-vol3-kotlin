package com.loopers.interfaces.api.user.auth

import com.loopers.application.user.auth.UserChangePasswordUseCase
import com.loopers.application.user.auth.UserMeUseCase
import com.loopers.application.user.auth.UserSignUpUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/users")
@RestController
class UserAuthV1Controller(
    private val userSignUpService: UserSignUpUseCase,
    private val userChangePasswordService: UserChangePasswordUseCase,
    private val userMeService: UserMeUseCase,
) : UserAuthV1ApiSpec {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    override fun signUp(
        @Valid @RequestBody request: UserAuthV1Request.SignUp,
    ): ApiResponse<UserAuthV1Response.SignUp> {
        return userSignUpService.signUp(request.toCommand())
            .let { UserAuthV1Response.SignUp.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: UserAuthV1Request.ChangePassword,
    ): ApiResponse<Nothing?> {
        userChangePasswordService.changePassword(loginId, password, request.toCommand())
        return ApiResponse.success(null)
    }

    @GetMapping("/me")
    override fun getMe(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<UserAuthV1Response.Me> {
        return userMeService.getMe(loginId, password)
            .let { UserAuthV1Response.Me.from(it) }
            .let { ApiResponse.success(it) }
    }
}
