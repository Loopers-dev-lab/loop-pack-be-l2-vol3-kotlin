package com.loopers.user.interfaces.api

import com.loopers.user.application.UserInfo
import com.loopers.user.application.model.UserSignUpCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate

class UserV1Dto {
    data class SignUpRequest(
        @field:NotBlank
        @field:Size(min = 4, max = 20)
        val loginId: String,
        @field:NotBlank
        @field:Size(min = 8, max = 16)
        val password: String,
        @field:NotBlank
        @field:Size(min = 2, max = 15)
        val name: String,
        @field:Past
        val birthDate: LocalDate,
        @field:NotBlank
        @field:Email
        val email: String,
    ) {
        fun toCommand(): UserSignUpCommand =
            UserSignUpCommand(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
    }

    data class SignUpResponse(
        val loginId: String,
    ) {
        companion object {
            fun from(info: UserInfo): SignUpResponse =
                SignUpResponse(
                    loginId = info.loginId,
                )
        }
    }
}
