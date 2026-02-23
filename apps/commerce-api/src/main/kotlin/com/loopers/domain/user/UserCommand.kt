package com.loopers.domain.user

import com.loopers.domain.user.entity.User
import java.time.LocalDate

class UserCommand {
    data class SignUp(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        fun toUser(): User {
            return User.create(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    data class ChangePassword(
        val currentPassword: String,
        val newPassword: String,
    )
}
