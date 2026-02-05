package com.loopers.application.user

import com.loopers.domain.user.User

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
    val gender: String,
) {
    companion object {
        fun from(user: User): UserInfo {
            val id = requireNotNull(user.persistenceId) {
                "User.persistenceId가 null입니다. 저장된 User만 매핑 가능합니다."
            }
            return UserInfo(
                id = id,
                loginId = user.loginId.value,
                name = user.name.value,
                birthDate = user.birthDate.value.toString(),
                email = user.email.value,
                gender = user.gender.name,
            )
        }
    }
}
