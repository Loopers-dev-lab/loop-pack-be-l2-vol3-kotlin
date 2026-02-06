package com.loopers.interfaces.api.auth

import com.loopers.application.auth.AuthFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthV1Controller(
    private val authFacade: AuthFacade,
) : AuthV1ApiSpec {

    @PostMapping("/login")
    override fun login(
        @RequestBody request: AuthV1Dto.LoginRequest,
    ): ApiResponse<AuthV1Dto.LoginResponse> {
        return authFacade.login(request.loginId, request.password)
            .let { AuthV1Dto.LoginResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
