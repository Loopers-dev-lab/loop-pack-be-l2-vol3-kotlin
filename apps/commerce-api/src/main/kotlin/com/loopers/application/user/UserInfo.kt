package com.loopers.application.user

import java.time.LocalDate

data class UserInfo(
    val loginId: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
