package com.loopers.application.user

import com.loopers.domain.user.User

data class UserInfo(
    val loginId: String,
    val name: String,
    val birth: String,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                loginId = user.loginId,
                name = user.name.substring(0, user.name.length - 1) + "*",
                birth = user.birth,
                email = user.email,
            )
        }
    }
}
