package com.loopers.user.interfaces.api

import com.loopers.example.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "User V1 API", description = "회원 API 입니다.")
interface UserV1ApiSpec {
    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    fun signUp(request: UserV1Dto.SignUpRequest): ResponseEntity<ApiResponse<UserV1Dto.SignUpResponse>>
}
