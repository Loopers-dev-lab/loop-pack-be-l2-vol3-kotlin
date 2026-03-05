package com.loopers.interfaces.api.user.auth

import com.loopers.application.user.auth.UserAuthCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate

class UserAuthV1Request {
    data class SignUp(
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
        fun toCommand(): UserAuthCommand.SignUp =
            UserAuthCommand.SignUp(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )

        override fun toString(): String =
            "UserAuthV1Request.SignUp(loginId=$loginId, password=[PROTECTED], name=$name, birthDate=$birthDate, email=$email)"
    }

    data class ChangePassword(
        @field:NotBlank
        val currentPassword: String,
        @field:NotBlank
        @field:Size(min = 8, max = 16)
        val newPassword: String,
    ) {
        fun toCommand(): UserAuthCommand.ChangePassword =
            UserAuthCommand.ChangePassword(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

        override fun toString(): String =
            "UserAuthV1Request.ChangePassword(currentPassword=[PROTECTED], newPassword=[PROTECTED])"
    }
}
