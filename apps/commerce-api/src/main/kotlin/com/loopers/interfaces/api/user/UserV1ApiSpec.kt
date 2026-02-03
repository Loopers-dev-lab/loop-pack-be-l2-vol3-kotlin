package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "회원 관련 API")
interface UserV1ApiSpec {

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    fun signUp(request: UserV1Dto.SignUpRequest): ApiResponse<UserV1Dto.UserResponse>

}
