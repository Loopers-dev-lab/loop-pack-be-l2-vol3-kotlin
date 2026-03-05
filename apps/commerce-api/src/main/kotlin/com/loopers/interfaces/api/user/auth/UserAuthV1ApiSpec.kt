package com.loopers.interfaces.api.user.auth

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User V1 API", description = "회원 API 입니다.")
interface UserAuthV1ApiSpec {
    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    fun signUp(request: UserAuthV1Request.SignUp): ApiResponse<UserAuthV1Response.SignUp>

    @Operation(
        summary = "내 정보 조회",
        description = "인증 헤더를 이용하여 내 정보를 조회합니다.",
    )
    fun getMe(loginId: String, password: String): ApiResponse<UserAuthV1Response.Me>

    @Operation(
        summary = "비밀번호 수정",
        description = "인증 헤더를 이용하여 비밀번호를 수정합니다.",
    )
    fun changePassword(
        loginId: String,
        password: String,
        request: UserAuthV1Request.ChangePassword,
    ): ApiResponse<Nothing?>
}
