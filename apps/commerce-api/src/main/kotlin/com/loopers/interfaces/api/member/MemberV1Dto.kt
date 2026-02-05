package com.loopers.interfaces.api.member

import com.loopers.domain.member.Member
import com.loopers.domain.member.SignUpCommand
import java.time.LocalDate

class MemberV1Dto {

    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
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

    data class SignUpResponse(
        val id: Long,
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

    data class MemberInfoResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
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

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )
}
