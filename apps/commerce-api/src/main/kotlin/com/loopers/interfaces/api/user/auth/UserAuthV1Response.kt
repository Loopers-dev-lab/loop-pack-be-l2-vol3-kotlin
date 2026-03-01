package com.loopers.interfaces.api.user.auth

import com.loopers.application.user.auth.UserResult
import java.time.LocalDate

class UserAuthV1Response {
    data class SignUp(
        val loginId: String,
    ) {
        companion object {
            fun from(info: UserResult.SignUp): SignUp =
                SignUp(loginId = info.loginId)
        }
    }

    data class Me(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: UserResult.Me): Me =
                Me(
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
        }
    }
}
