package com.loopers.application.user

import com.loopers.domain.user.MaskedName
import com.loopers.domain.user.User
import java.time.LocalDate

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val email: String,
    val maskedName: String? = null,
    val birthday: LocalDate? = null,
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                loginId = user.loginId.value,
                name = user.name,
                email = user.email.value,
            )
        }

        fun fromWithMasking(userInfo: AuthenticatedUserInfo): UserInfo {
            return UserInfo(
                id = userInfo.id,
                loginId = userInfo.loginId,
                name = userInfo.name,
                email = userInfo.email,
                maskedName = MaskedName.from(userInfo.name).value,
                birthday = userInfo.birthday,
            )
        }
    }
}
