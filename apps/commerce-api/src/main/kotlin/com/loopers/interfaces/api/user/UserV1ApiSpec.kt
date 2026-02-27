package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "User V1 API", description = "회원 관련 API")
interface UserV1ApiSpec {

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "회원가입 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 8자 미만, 로그인 ID 10자 초과, 이메일 형식 오류 등)"),
            SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 로그인 ID"),
        ],
    )
    fun signUp(request: UserV1Dto.SignUpRequest): ApiResponse<Any>

    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (로그인 ID 없음 또는 비밀번호 불일치)"),
        ],
    )
    fun getMyInfo(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
    ): ApiResponse<UserV1Dto.UserInfoResponse>

    @Operation(summary = "비밀번호 변경", description = "인증된 사용자의 비밀번호를 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (새 비밀번호 8자 미만, 현재 비밀번호와 동일)"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (현재 비밀번호 불일치)"),
        ],
    )
    fun changePassword(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        request: UserV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any>
}
