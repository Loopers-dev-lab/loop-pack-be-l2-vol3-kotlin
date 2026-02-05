package com.loopers.domain.member

import java.time.LocalDate

data class RegisterCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
