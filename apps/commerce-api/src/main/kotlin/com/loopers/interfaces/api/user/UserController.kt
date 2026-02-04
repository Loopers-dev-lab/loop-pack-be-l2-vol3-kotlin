package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userFacade: UserFacade,
) : UserApiSpec {
    companion object {
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
    }

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
        @RequestHeader(LOGIN_ID_HEADER, required = false) loginId: String?,
        @RequestHeader(LOGIN_PW_HEADER, required = false) password: String?,
    ): ApiResponse<UserDto.MeResponse> {
        if (loginId.isNullOrBlank() || password.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더가 필요합니다.")
        }

        return userFacade.getMe(loginId, password)
            .let { UserDto.MeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
