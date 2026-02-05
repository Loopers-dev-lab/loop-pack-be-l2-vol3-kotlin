package com.loopers.interfaces.api.auth

import com.loopers.application.auth.AuthFacade
import com.loopers.application.auth.AuthInfo
import java.time.LocalDate

class AuthV1Dto {

    data class SignupRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        fun toCommand() = AuthFacade.SignupCommand(
            loginId = loginId,
            rawPassword = password,
            name = name,
            birthDate = birthDate,
            email = email,
        )
    }

    data class SignupResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: AuthInfo.SignupResult) = SignupResponse(
                id = info.id,
                loginId = info.loginId,
                name = info.name,
                birthDate = info.birthDate,
                email = info.email,
            )
        }
    }
}
