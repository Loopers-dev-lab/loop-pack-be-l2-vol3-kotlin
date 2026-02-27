package com.loopers.application.user

import java.time.LocalDate

data class UserMeInfo(
    val id: Long,
    val loginId: String,
    val maskedName: String,
    val email: String,
    val birthday: LocalDate,
)
