package com.loopers.interfaces.api.member

import com.loopers.domain.member.AuthService
import com.loopers.domain.member.MemberService
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Member V1 API", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberService: MemberService,
    private val authService: AuthService,
) {

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "회원가입 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 8자 미만, 로그인 ID 10자 초과, 이메일 형식 오류 등)"),
            SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 로그인 ID"),
        ],
    )
    @PostMapping("/signup")
    fun signUp(
        @RequestBody request: MemberV1Dto.SignUpRequest,
    ): ApiResponse<MemberV1Dto.SignUpResponse> {
        return memberService.signUp(request.toCommand())
            .let { MemberV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (로그인 ID 없음 또는 비밀번호 불일치)"),
        ],
    )
    @GetMapping("/me")
    fun getMyInfo(
        @Parameter(description = "로그인 ID", required = true)
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @Parameter(description = "비밀번호", required = true)
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<MemberV1Dto.MemberInfoResponse> {
        val member = authService.authenticate(loginId, password)
        return MemberV1Dto.MemberInfoResponse.from(member)
            .let { ApiResponse.success(it) }
    }

    @Operation(summary = "비밀번호 변경", description = "인증된 사용자의 비밀번호를 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (새 비밀번호 8자 미만, 현재 비밀번호와 동일)"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (현재 비밀번호 불일치)"),
        ],
    )
    @PatchMapping("/me/password")
    fun changePassword(
        @Parameter(description = "로그인 ID", required = true)
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @Parameter(description = "비밀번호", required = true)
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        val member = authService.authenticate(loginId, password)
        memberService.changePassword(member.id, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
