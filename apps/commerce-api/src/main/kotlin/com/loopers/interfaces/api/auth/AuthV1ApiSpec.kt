package com.loopers.interfaces.api.auth

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Auth V1 API", description = "인증 관련 API")
interface AuthV1ApiSpec {

    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "회원가입 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "중복된 로그인 ID",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun signup(request: AuthV1Dto.SignupRequest): ApiResponse<AuthV1Dto.SignupResponse>
}
