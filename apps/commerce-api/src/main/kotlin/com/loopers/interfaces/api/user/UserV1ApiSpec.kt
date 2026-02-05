package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "사용자 관련 API 입니다.")
interface UserV1ApiSpec {

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자 계정을 생성합니다.",
    )
    fun signup(request: UserV1Dto.SignupRequest): ApiResponse<UserV1Dto.UserResponse>

    @Operation(
        summary = "내 정보 조회",
        description = "로그인된 사용자의 정보를 조회합니다. 이름은 마스킹됩니다.",
    )
    fun getMyInfo(loginId: String, loginPw: String): ApiResponse<UserV1Dto.UserResponse>

    @Operation(
        summary = "비밀번호 수정",
        description = "로그인된 사용자의 비밀번호를 수정합니다.",
    )
    fun changePassword(loginId: String, loginPw: String, request: UserV1Dto.UserChangePasswordRequest): ApiResponse<Any>
}
