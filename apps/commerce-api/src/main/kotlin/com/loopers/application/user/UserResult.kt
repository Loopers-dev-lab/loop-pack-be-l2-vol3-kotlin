package com.loopers.application.user

import java.time.LocalDate

data class UserSignUpResult(
    val loginId: String,
)

data class UserMeResult(
    val loginId: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
