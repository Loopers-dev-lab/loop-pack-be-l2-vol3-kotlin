package com.loopers.application.user

import java.time.LocalDate

class UserCommand {

    data class Register(
        val loginId: String,
        val rawPassword: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    )

    data class ChangePassword(
        val userId: Long,
        val currentPassword: String,
        val newPassword: String,
    )
}
