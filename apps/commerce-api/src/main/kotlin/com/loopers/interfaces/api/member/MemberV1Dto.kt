package com.loopers.interfaces.api.member

import com.loopers.domain.member.MemberModel
import java.time.LocalDate

class MemberV1Dto {
    data class RegisterRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val email: String,
        val birthDate: LocalDate,
    )

    data class RegisterResponse(
        val loginId: String,
        val name: String,
        val email: String,
        val birthDate: LocalDate,
    ) {
        companion object {
            fun from(member: MemberModel): RegisterResponse {
                return RegisterResponse(
                    loginId = member.loginId,
                    name = member.name,
                    email = member.email,
                    birthDate = member.birthDate,
                )
            }
        }
    }

    data class MemberResponse(
        val loginId: String,
        val name: String,
        val email: String,
        val birthDate: LocalDate,
    ) {
        companion object {
            fun from(member: MemberModel): MemberResponse {
                return MemberResponse(
                    loginId = member.loginId,
                    name = member.getMaskedName(),
                    email = member.email,
                    birthDate = member.birthDate,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )
}
