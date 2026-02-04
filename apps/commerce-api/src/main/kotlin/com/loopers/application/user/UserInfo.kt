package com.loopers.application.user

import com.loopers.domain.example.User

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                loginId = user.loginId,
                name = user.name,
                email = user.email,
            )
        }
    }
}
