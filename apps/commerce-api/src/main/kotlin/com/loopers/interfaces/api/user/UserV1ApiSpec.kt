package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "사용자 관련 API 입니다.")
interface UserV1ApiSpec {
    @Operation(
        summary = "회원 가입",
        description = "새로운 사용자를 등록합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "등록 성공")
    fun register(
        request: UserV1Dto.RegisterRequest,
    )

    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 사용자의 정보를 조회합니다. 이름은 마지막 글자가 마스킹됩니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getMe(
        loginId: String,
        loginPw: String,
    ): ApiResponse<UserV1Dto.UserResponse>

    @Operation(
        summary = "비밀번호 수정",
        description = "로그인한 사용자의 비밀번호를 수정합니다.",
    )
    @SwaggerResponse(responseCode = "204", description = "수정 성공")
    fun updatePassword(
        loginId: String,
        loginPw: String,
        request: UserV1Dto.UpdatePasswordRequest,
    )
}
