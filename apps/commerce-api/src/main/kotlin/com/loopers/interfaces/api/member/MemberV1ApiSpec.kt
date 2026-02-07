package com.loopers.interfaces.api.member

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member V1 API", description = "회원 관련 API")
interface MemberV1ApiSpec {

    @Operation(
        summary = "내 정보 조회",
        description = "로그인한 회원의 정보를 조회합니다. 이름은 마지막 글자가 마스킹됩니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun getMyProfile(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
    ): ApiResponse<MemberV1Dto.MyProfileResponse>

    @Operation(
        summary = "비밀번호 변경",
        description = "로그인한 회원의 비밀번호를 변경합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "변경 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 비밀번호",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiResponse::class))],
            ),
        ],
    )
    fun changePassword(
        @Parameter(hidden = true) loginId: String,
        @Parameter(hidden = true) password: String,
        request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any>
}
