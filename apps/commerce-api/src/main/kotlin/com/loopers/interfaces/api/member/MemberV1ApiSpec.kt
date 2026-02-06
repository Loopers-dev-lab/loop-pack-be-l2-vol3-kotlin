package com.loopers.interfaces.api.member

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest

@Tag(name = "Member V1 API", description = "회원 관련 API입니다.")
interface MemberV1ApiSpec {
    @Operation(
        summary = "내 정보 조회",
        description = "인증된 사용자의 정보를 마스킹하여 조회합니다.",
    )
    fun getMyInfo(request: HttpServletRequest): ApiResponse<MemberV1Dto.MyInfoResponse>

    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    fun signUp(request: MemberV1Dto.SignUpRequest): ApiResponse<MemberV1Dto.SignUpResponse>

    @Operation(
        summary = "비밀번호 변경",
        description = "인증된 사용자의 비밀번호를 변경합니다.",
    )
    fun changePassword(
        request: HttpServletRequest,
        body: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any>
}
