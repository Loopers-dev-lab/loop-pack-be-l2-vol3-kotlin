package com.loopers.interfaces.api.auth

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Auth V1 API", description = "인증 관련 API입니다.")
interface AuthV1ApiSpec {
    @Operation(
        summary = "로그인",
        description = "로그인하여 JWT 토큰을 발급받습니다.",
    )
    fun login(request: AuthV1Dto.LoginRequest): ApiResponse<AuthV1Dto.LoginResponse>
}
