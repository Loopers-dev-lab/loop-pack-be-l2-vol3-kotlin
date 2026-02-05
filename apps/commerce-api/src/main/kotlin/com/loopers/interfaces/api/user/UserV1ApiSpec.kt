package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "회원 API")
interface UserV1ApiSpec {
    @Operation(
        summary = "회원 가입",
        description = "로그인 ID, 비밀번호, 이름, 생일, 이메일 정보로 회원 가입 시도",
    )
    fun registerUser(req: UserV1Dto.RegisterUserRequest): ApiResponse<UserV1Dto.UserResponse>

    @Operation(
        summary = "내 정보 조회",
        description = "내 정보를 조회합니다.",
    )
    fun getUserInfo(loginId: String, password: String): ApiResponse<UserV1Dto.UserResponse>
}
