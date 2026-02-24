package com.loopers.application.user

import java.time.LocalDate

data class UserSignUpInfo(
    val loginId: String,
)

data class UserMeInfo(
    val loginId: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
