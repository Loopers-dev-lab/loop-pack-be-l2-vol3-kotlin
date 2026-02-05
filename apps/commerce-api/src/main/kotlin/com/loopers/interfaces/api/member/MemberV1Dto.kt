package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import java.time.LocalDate

class MemberV1Dto {
    data class RegisterRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    )

    data class ChangePasswordRequest(
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
