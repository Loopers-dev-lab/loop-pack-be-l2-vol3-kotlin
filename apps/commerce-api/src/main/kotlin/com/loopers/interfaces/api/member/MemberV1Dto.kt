package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

class MemberV1Dto {
    data class RegisterRequest(
        @field:NotBlank(message = "로그인 ID는 필수입니다.")
        val loginId: String,
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        val password: String,
        @field:NotBlank(message = "이름은 필수입니다.")
        val name: String,
        val birthday: LocalDate,
        @field:NotBlank(message = "이메일은 필수입니다.")
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        val email: String,
    )

    data class ChangePasswordRequest(
        @field:NotBlank(message = "새 비밀번호는 필수입니다.")
        val newPassword: String,
    )

    data class MemberResponse(
        val loginId: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): MemberResponse {
                return MemberResponse(
                    loginId = info.loginId,
                    name = info.name,
                    birthday = info.birthday,
                    email = info.email,
                )
            }
        }
    }
}
