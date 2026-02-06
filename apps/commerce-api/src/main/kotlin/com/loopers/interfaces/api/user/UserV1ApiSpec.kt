package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "User V1 API", description = "유저 API 입니다.")
interface UserV1ApiSpec {

    @Operation(
        summary = "회원가입",
        description = "새로운 유저를 등록합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "회원가입 성공"),
            SwaggerResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식, 비밀번호 규칙 등)"),
            SwaggerResponse(responseCode = "409", description = "이미 존재하는 로그인 ID"),
        ],
    )
    fun signUp(
        request: UserV1Dto.SignUpRequest,
    ): ApiResponse<UserV1Dto.UserResponse>

    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 유저의 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 유저"),
        ],
    )
    fun getMe(
        userId: Long,
    ): ApiResponse<UserV1Dto.UserResponse>
}
