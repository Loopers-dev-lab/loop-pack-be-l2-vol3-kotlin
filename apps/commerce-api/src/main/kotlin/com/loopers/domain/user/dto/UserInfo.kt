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
                loginId = user.loginId.value,
                name = maskLastCharacter(user.name.value),
                birthDate = user.birthDate.value,
                email = user.email.value,
            )
        }

        private fun maskLastCharacter(name: String): String {
            if (name.length <= 1) return "*"
            return name.dropLast(1) + "*"
        }
    }
}
