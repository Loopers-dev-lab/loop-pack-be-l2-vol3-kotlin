package com.loopers.application.user

import java.time.LocalDate

data class SignUpCriteria(
    val loginId: String,
    val password: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
)

data class ChangePasswordCriteria(
    val newPassword: String,
)
