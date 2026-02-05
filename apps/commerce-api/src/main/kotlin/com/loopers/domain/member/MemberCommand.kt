package com.loopers.domain.member

import java.time.LocalDate

class MemberCommand {
    data class Register(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    )

    data class Authenticate(
        val loginId: String,
        val password: String,
    )

    data class ChangePassword(
        val memberId: Long,
        val currentPassword: String,
        val newPassword: String,
    )
}
