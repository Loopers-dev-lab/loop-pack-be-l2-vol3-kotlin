package com.loopers.application.user

import com.loopers.domain.user.User
import java.time.LocalDate

data class AuthenticatedUserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val email: String,
    val birthday: LocalDate,
) {
    companion object {
        fun from(user: User): AuthenticatedUserInfo {
            return AuthenticatedUserInfo(
                id = user.id,
                loginId = user.loginId.value,
                name = user.name,
                email = user.email.value,
                birthday = user.birthday,
            )
        }
    }
}
