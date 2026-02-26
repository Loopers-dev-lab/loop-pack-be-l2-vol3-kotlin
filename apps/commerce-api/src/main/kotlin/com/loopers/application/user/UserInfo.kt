package com.loopers.application.user

import com.loopers.domain.user.User
import java.time.LocalDate

data class AuthenticatedUserInfo(
    val id: Long,
) {
    companion object {
        fun from(user: User): AuthenticatedUserInfo {
            return AuthenticatedUserInfo(id = user.id)
        }
    }
}

data class UserInfo(
    val id: Long,
    val loginId: String,
    val maskedName: String,
    val birthDate: LocalDate,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                loginId = user.loginId,
                maskedName = user.getMaskedName(),
                birthDate = user.birthDate,
                email = user.email,
            )
        }
    }
}
