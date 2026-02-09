package com.loopers.application.user

import com.loopers.domain.user.User
import java.time.LocalDate

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                loginId = user.loginId,
                name = user.maskedName(),
                birthday = user.birthday,
                email = user.email,
            )
        }
    }
}
