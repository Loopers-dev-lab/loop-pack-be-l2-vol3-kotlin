package com.loopers.user.application

import java.time.LocalDate

data class UserInfo(
    val loginId: String,
    val name: String? = null,
    val birthDate: LocalDate? = null,
    val email: String? = null,
)
