package com.loopers.application.user.model

import java.time.LocalDate

data class UserSignUpCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
) {
    override fun toString(): String =
        "UserSignUpCommand(loginId=$loginId, password=[PROTECTED], name=$name, birthDate=$birthDate, email=$email)"
}
