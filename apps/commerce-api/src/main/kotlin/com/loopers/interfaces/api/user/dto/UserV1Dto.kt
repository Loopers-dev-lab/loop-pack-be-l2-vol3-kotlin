package com.loopers.interfaces.api.user.dto

import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.entity.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

class UserV1Dto {
    data class SignUpRequest(
        @field:NotBlank(message = "로그인 ID는 필수입니다.")
        @field:Size(min = 4, max = 16, message = "로그인 ID는 4~16자여야 합니다.")
        @field:Pattern(regexp = "^[a-zA-Z0-9]+$", message = "로그인 ID는 영문 및 숫자만 허용됩니다.")
        val loginId: String,
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val password: String,
        @field:NotBlank(message = "이름은 필수입니다.")
        @field:Size(max = 10, message = "이름은 10자 이내여야 합니다.")
        @field:Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문만 허용됩니다.")
        val name: String,
        val birthDate: LocalDate,
        @field:NotBlank(message = "이메일은 필수입니다.")
        @field:Email(message = "이메일 형식이 올바르지 않습니다.")
        val email: String,
    ) {
        fun toCommand(): UserCommand.SignUp {
            return UserCommand.SignUp(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    data class UserResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(user: User): UserResponse {
                return UserResponse(
                    loginId = user.loginId,
                    name = user.name,
                    birthDate = user.birthDate,
                    email = user.email,
                )
            }

            fun fromWithMaskedName(user: User): UserResponse {
                return UserResponse(
                    loginId = user.loginId,
                    name = user.getMaskedName(),
                    birthDate = user.birthDate,
                    email = user.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val currentPassword: String,
        @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
        val newPassword: String,
    ) {
        fun toCommand(): UserCommand.ChangePassword {
            return UserCommand.ChangePassword(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        }
    }
}
