package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): UserInfo {
        return userService.signUp(loginId, password, name, email, birthday)
            .let { UserInfo.from(it) }
    }

    fun authenticate(loginId: String, password: String): AuthenticatedUserInfo {
        return userService.authenticate(loginId, password)
            .let { AuthenticatedUserInfo.from(it) }
    }

    fun getMe(userInfo: AuthenticatedUserInfo): UserInfo {
        return UserInfo.fromWithMasking(userInfo)
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        userService.changePassword(userId, currentPassword, newPassword)
    }
}
