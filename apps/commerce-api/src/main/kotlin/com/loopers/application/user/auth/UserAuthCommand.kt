package com.loopers.application.user.auth

class UserAuthCommand {
    data class SignUp(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: java.time.LocalDate,
        val email: String,
    ) {
        override fun toString(): String =
            "UserAuthCommand.SignUp(loginId=$loginId, password=[PROTECTED], name=$name, birthDate=$birthDate, email=$email)"
    }

    data class ChangePassword(
        val currentPassword: String,
        val newPassword: String,
    ) {
        override fun toString(): String =
            "UserAuthCommand.ChangePassword(currentPassword=[PROTECTED], newPassword=[PROTECTED])"
    }
}
