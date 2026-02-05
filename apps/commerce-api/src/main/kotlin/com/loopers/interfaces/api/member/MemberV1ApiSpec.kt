package com.loopers.interfaces.api.member

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member V1", description = "회원 관리 API V1")
interface MemberV1ApiSpec {

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    fun register(request: MemberV1Dto.RegisterRequest): ApiResponse<*>

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    fun getMyInfo(
        @Parameter(description = "로그인 ID", required = true) loginId: String,
        @Parameter(description = "비밀번호", required = true) password: String,
    ): ApiResponse<MemberV1Dto.MemberResponse>

    @Operation(summary = "비밀번호 변경", description = "로그인한 회원의 비밀번호를 변경합니다.")
    fun changePassword(
        @Parameter(description = "로그인 ID", required = true) loginId: String,
        @Parameter(description = "현재 비밀번호", required = true) currentPassword: String,
        request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<*>
}
