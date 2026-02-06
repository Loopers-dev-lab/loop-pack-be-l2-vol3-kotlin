package com.loopers.interfaces.api.member

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member V1 API", description = "회원 관련 API입니다.")
interface MemberV1ApiSpec {
    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    fun signUp(request: MemberV1Dto.SignUpRequest): ApiResponse<MemberV1Dto.SignUpResponse>
}
