package com.loopers.interfaces.api.member

import com.loopers.domain.member.Member
import com.loopers.domain.member.SignUpCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

class MemberV1Dto {

    @Schema(description = "회원가입 요청")
    data class SignUpRequest(
        @Schema(description = "로그인 ID (최대 10자)", example = "testuser1")
        val loginId: String,
        @Schema(description = "비밀번호 (최소 8자, 영문+숫자+특수문자 포함)", example = "Password1!")
        val password: String,
        @Schema(description = "이름", example = "홍길동")
        val name: String,
        @Schema(description = "생년월일", example = "1990-01-15")
        val birthDate: LocalDate,
        @Schema(description = "이메일", example = "test@example.com")
        val email: String,
    ) {
        fun toCommand(): SignUpCommand {
            return SignUpCommand(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    @Schema(description = "회원가입 응답")
    data class SignUpResponse(
        @Schema(description = "회원 ID", example = "1")
        val id: Long,
        @Schema(description = "로그인 ID", example = "testuser1")
        val loginId: String,
    ) {
        companion object {
            fun from(member: Member): SignUpResponse {
                return SignUpResponse(
                    id = member.id,
                    loginId = member.loginId,
                )
            }
        }
    }

    @Schema(description = "회원 정보 응답")
    data class MemberInfoResponse(
        @Schema(description = "로그인 ID", example = "testuser1")
        val loginId: String,
        @Schema(description = "이름 (마스킹 처리)", example = "홍길*")
        val name: String,
        @Schema(description = "생년월일", example = "1990-01-15")
        val birthDate: LocalDate,
        @Schema(description = "이메일", example = "test@example.com")
        val email: String,
    ) {
        companion object {
            fun from(member: Member): MemberInfoResponse {
                return MemberInfoResponse(
                    loginId = member.loginId,
                    name = member.getMaskedName(),
                    birthDate = member.birthDate,
                    email = member.email,
                )
            }
        }
    }

    @Schema(description = "비밀번호 변경 요청")
    data class ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "Password1!")
        val currentPassword: String,
        @Schema(description = "새 비밀번호 (최소 8자, 영문+숫자+특수문자 포함)", example = "NewPassword1!")
        val newPassword: String,
    )
}
