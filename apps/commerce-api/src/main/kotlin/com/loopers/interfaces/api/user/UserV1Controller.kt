package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.domain.user.dto.UserInfo
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class UserV1Controller(
    private val userFacade: UserFacade,
) : UserV1ApiSpec {

    @PostMapping("/user/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    override fun signUp(@RequestBody signUpRequest: UserV1Dto.SignUpRequest): ApiResponse<Any> {
        val signUpCommand = SignUpCommand(
            loginId = signUpRequest.loginId,
            password = signUpRequest.password,
            name = signUpRequest.name,
            birthDate = signUpRequest.birthDate,
            email = signUpRequest.email,
        )
        userFacade.signUp(signUpCommand)
        return ApiResponse.success()
    }

    @GetMapping("/user")
    override fun findUserInfo(
        @AuthenticationPrincipal id: Long,
    ): ApiResponse<UserInfo> = userFacade.findUserInfo(id).let { ApiResponse.success(it) }

    @PutMapping("/user/password")
    override fun changePassword(
        @AuthenticationPrincipal id: Long,
        @RequestBody passwordChangeRequest: UserV1Dto.PasswordChangeRequest,
    ): ApiResponse<Any> {
        userFacade.changePassword(id, passwordChangeRequest.currentPassword, passwordChangeRequest.newPassword)
        return ApiResponse.success()
    }
}
