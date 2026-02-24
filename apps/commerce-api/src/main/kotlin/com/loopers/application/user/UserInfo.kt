package com.loopers.application.user

import com.loopers.domain.user.model.User
import java.time.LocalDate

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val maskedName: String,
    val birthDate: LocalDate,
    val email: String,
) {
    companion object {
        fun from(user: User): UserInfo = UserInfo(
            id = user.id,
            loginId = user.loginId.value,
            name = user.name.value,
            maskedName = user.getMaskedName(),
            birthDate = user.birthDate,
            email = user.email.value,
        )
    }
}
