package com.loopers.interfaces.api.user.dto

import com.loopers.application.user.UserInfo
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

class UserV1Dto {
    data class SignUpRequest(
        @field:NotBlank(message = "로그인 ID는 필수입니다.")
        @field:Size(min = 4, max = 16, message = "로그인 ID는 4~16자여야 합니다.")
        @field:Pattern(regexp = "^[a-zA-Z0-9]+$", message = "로그인 ID는 영문 및 숫자만 허용됩니다.")
        val loginId: String,
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val password: String,
        @field:NotBlank(message = "이름은 필수입니다.")
        @field:Size(max = 10, message = "이름은 10자 이내여야 합니다.")
        @field:Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 허용됩니다.")
        val name: String,
        @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
        val birthDate: LocalDate,
        @field:NotBlank(message = "이메일은 필수입니다.")
        @field:Email(message = "이메일 형식이 올바르지 않습니다.")
        val email: String,
    )

    data class UserResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }

            fun fromMasked(info: UserInfo): UserResponse {
                return UserResponse(
                    loginId = info.loginId,
                    name = info.maskedName,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val currentPassword: String,
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val newPassword: String,
    )
}
