package com.loopers.application.user

import com.loopers.domain.user.UserModel

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
) {
    companion object {
        fun from(model: UserModel): UserInfo {
            return UserInfo(
                id = model.id,
                loginId = model.loginId.value,
                name = model.name.value,
                birthDate = model.birthDate.value,
                email = model.email.value,
            )
        }
    }
}
