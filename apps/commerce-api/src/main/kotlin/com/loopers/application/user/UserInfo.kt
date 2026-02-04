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
        /**
         * Create a UserInfo DTO from a domain User.
         *
         * @param user The domain User to convert.
         * @return A UserInfo populated with the user's persistence id, loginId, name, birthDate (as a string), email, and gender name.
         */
        fun from(user: User) = UserInfo(
            id = user.persistenceId!!,
            loginId = user.loginId.value,
            name = user.name.value,
            birthDate = user.birthDate.value.toString(),
            email = user.email.value,
            gender = user.gender.name,
        )
    }
}