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

    @PostMapping("/signup")
    override fun signup(
        @RequestBody request: AuthV1Dto.SignupRequest,
    ): ApiResponse<AuthV1Dto.SignupResponse> {
        return authFacade.signup(request.toCommand())
            .let { AuthV1Dto.SignupResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
