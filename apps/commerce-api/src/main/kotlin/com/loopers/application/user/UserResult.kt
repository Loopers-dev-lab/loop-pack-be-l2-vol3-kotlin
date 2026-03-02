package com.loopers.application.user

import com.loopers.domain.user.UserInfo
import java.time.LocalDate

data class UserResult(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
) {
    companion object {
        fun from(info: UserInfo): UserResult {
            return UserResult(
                id = info.id,
                loginId = info.loginId,
                name = info.name,
                birthday = info.birthday,
                email = info.email,
            )
        }
    }
}
