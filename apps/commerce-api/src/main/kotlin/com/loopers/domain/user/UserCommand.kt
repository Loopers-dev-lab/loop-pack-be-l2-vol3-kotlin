package com.loopers.domain.user

import java.time.LocalDate

data class SignUpCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
)

data class ChangePasswordCommand(
    val newPassword: String,
)
