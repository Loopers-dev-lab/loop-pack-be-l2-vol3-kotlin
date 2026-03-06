package com.loopers.interfaces.api.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class UserV1Dto {

    data class UserInfo(
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    )

    data class SignUpRequest(
        @field:NotBlank(message = "로그인 ID는 비어있을 수 없습니다.")
        @field:Size(min = 4, max = 20, message = "로그인 ID는 4자 이상 20자 이하여야 합니다.")
        @field:Pattern(regexp = "^[a-zA-Z0-9]+$", message = "로그인 ID는 영문과 숫자로만 이루어져야 합니다.")
        val loginId: String,

        @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
        val password: String,

        @field:NotBlank(message = "이름은 비어있을 수 없습니다.")
        @field:Size(min = 2, message = "이름은 2글자 이상이어야 합니다.")
        val name: String,

        @field:NotBlank(message = "생년월일은 비어있을 수 없습니다.")
        @field:Pattern(regexp = "^\\d{8}$", message = "생년월일은 yyyyMMdd 형식이어야 합니다.")
        val birthDate: String,

        @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
        @field:Email(message = "이메일은 이메일 형식에 맞아야 합니다.")
        val email: String,
    )

    data class PasswordChangeRequest(
        @field:NotBlank(message = "현재 비밀번호는 비어있을 수 없습니다.")
        val currentPassword: String,

        @field:NotBlank(message = "새로운 비밀번호는 비어있을 수 없습니다.")
        val newPassword: String,
    )
}
