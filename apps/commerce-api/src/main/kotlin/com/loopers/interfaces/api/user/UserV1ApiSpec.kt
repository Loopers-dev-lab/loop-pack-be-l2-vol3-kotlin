package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.HEADER_LOGIN_ID
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestHeader

@Tag(name = "User V1 API", description = "회원 관련 API")
interface UserV1ApiSpec {

    @SecurityRequirements
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    fun signUp(@RequestBody request: UserV1Dto.SignUpRequest): ApiResponse<UserV1Dto.UserResponse>

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    fun getUserInfo(
        @Parameter(hidden = true)
        @RequestHeader(name = HEADER_LOGIN_ID, required = true) loginId: String,
    ): ApiResponse<UserV1Dto.UserResponse>

    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 회원의 비밀번호를 변경합니다.")
    fun changePassword(
        @Parameter(hidden = true)
        @RequestHeader(name = HEADER_LOGIN_ID, required = true) loginId: String,
        @RequestBody request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any>
}
