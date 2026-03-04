package com.loopers.support.auth

import java.time.LocalDate

data class AuthenticatedUserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val email: String,
    val birthday: LocalDate,
)
