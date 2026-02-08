package com.loopers.user.application.model

import java.time.LocalDate

data class UserSignUpCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
