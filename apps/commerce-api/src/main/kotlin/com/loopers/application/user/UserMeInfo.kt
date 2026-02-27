package com.loopers.application.user

import com.loopers.domain.user.MaskedName
import com.loopers.support.auth.AuthenticatedUserInfo
import java.time.LocalDate

data class UserMeInfo(
    val id: Long,
    val loginId: String,
    val maskedName: String,
    val email: String,
    val birthday: LocalDate,
) {
    companion object {
        fun from(userInfo: AuthenticatedUserInfo): UserMeInfo {
            return UserMeInfo(
                id = userInfo.id,
                loginId = userInfo.loginId,
                maskedName = MaskedName.from(userInfo.name).value,
                email = userInfo.email,
                birthday = userInfo.birthday,
            )
        }
    }
}
