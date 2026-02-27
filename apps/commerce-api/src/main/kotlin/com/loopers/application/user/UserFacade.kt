package com.loopers.application.user

import com.loopers.domain.user.MaskedName
import com.loopers.domain.user.UserService
import com.loopers.support.auth.AuthenticatedUserInfo
import com.loopers.support.auth.Authenticator
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class UserFacade(
    private val userService: UserService,
) : Authenticator {
    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): UserInfo {
        return userService.signUp(loginId, password, name, email, birthday)
            .let { UserInfo.from(it) }
    }

    override fun authenticate(loginId: String, password: String): AuthenticatedUserInfo {
        val user = userService.authenticate(loginId, password)
        return AuthenticatedUserInfo(
            id = user.id,
            loginId = user.loginId.value,
            name = user.name,
            email = user.email.value,
            birthday = user.birthday,
        )
    }

    fun getMe(userInfo: AuthenticatedUserInfo): UserMeInfo {
        val maskedName = MaskedName.from(userInfo.name)
        return UserMeInfo(
            id = userInfo.id,
            loginId = userInfo.loginId,
            maskedName = maskedName.value,
            email = userInfo.email,
            birthday = userInfo.birthday,
        )
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        userService.changePassword(userId, currentPassword, newPassword)
    }
}
