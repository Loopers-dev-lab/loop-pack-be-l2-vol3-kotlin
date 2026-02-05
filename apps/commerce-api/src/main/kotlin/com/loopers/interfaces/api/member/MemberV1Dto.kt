package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

class MemberV1Dto {
    data class RegisterRequest(
        @field:NotBlank(message = "로그인ID는 비어있을 수 없습니다.")
        val loginId: String,
        @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
        val password: String,
        @field:NotBlank(message = "이름은 비어있을 수 없습니다.")
        val name: String,
        val birthDate: LocalDate,
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
        val email: String,
    )

    data class MemberResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): MemberResponse {
                return MemberResponse(
                    loginId = info.loginId,
                    name = info.getMaskedName(),
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        @field:NotBlank(message = "새 비밀번호는 비어있을 수 없습니다.")
        val newPassword: String,
    )
}
