package com.loopers.application.user.auth

import java.time.LocalDate

class UserResult {
    data class SignUp(
        val loginId: String,
    )

    data class Me(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    )
}
