package com.loopers.domain.user.dto

import com.loopers.domain.user.User

data class UserInfo(
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                loginId = user.loginId,
                name = maskLastCharacter(user.name),
                birthDate = user.birthDate,
                email = user.email,
            )
        }

        private fun maskLastCharacter(name: String): String {
            if (name.length <= 1) return "*"
            return name.dropLast(1) + "*"
        }
    }
}
