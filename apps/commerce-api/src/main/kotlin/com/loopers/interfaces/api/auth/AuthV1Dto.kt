package com.loopers.interfaces.api.auth

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.auth.AuthInfo
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

class AuthV1Dto {

    data class SignupRequest(
        @field:NotBlank val loginId: String,
        @field:NotBlank val password: String,
        @field:NotBlank val name: String,
        val birthDate: LocalDate,
        @field:NotBlank val email: String,
    ) {
        fun toCommand() = AuthUseCase.SignupCommand(
            loginId = loginId,
            rawPassword = password,
            name = name,
            birthDate = birthDate,
            email = email,
        )

        override fun toString(): String =
            "SignupRequest(loginId=$loginId, password=****, name=$name, birthDate=$birthDate, email=$email)"
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
