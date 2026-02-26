package com.loopers.interfaces.api.user

import com.loopers.application.user.SignUpCriteria
import com.loopers.application.user.UserInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

class UserV1Dto {

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
        fun toCriteria(): SignUpCriteria {
            return SignUpCriteria(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    @Schema(description = "회원 정보 응답")
    data class UserInfoResponse(
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
            fun from(info: UserInfo): UserInfoResponse {
                return UserInfoResponse(
                    loginId = info.loginId,
                    name = info.maskedName,
                    birthDate = info.birthDate,
                    email = info.email,
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
