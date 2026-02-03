package com.loopers.application.user

import com.loopers.domain.user.UserModel
import java.time.ZonedDateTime

data class UserInfo(
    val id: Long,
    val username: String,
    val name: String,
    val email: String,
    val birthDate: ZonedDateTime,
) {
    companion object {
        fun from(model: UserModel): UserInfo {
            return UserInfo(
                id = model.id,
                username = model.username,
                name = model.name,
                email = model.email,
                birthDate = model.birthDate,
            )
        }
    }
}
