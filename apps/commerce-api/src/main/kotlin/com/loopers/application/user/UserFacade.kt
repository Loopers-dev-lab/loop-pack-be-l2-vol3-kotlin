package com.loopers.application.user

import com.loopers.application.user.UserService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthDate: String,
        email: String,
    ): UserInfo {
        return userService.createUser(
            loginId = loginId,
            rawPassword = password,
            name = name,
            birthDate = birthDate,
            email = email,
        ).let { UserInfo.from(it) }
    }

    fun getUserInfo(loginId: String): UserInfo {
        return userService.getUserByLoginId(loginId)
            .let { UserInfo.from(it) }
    }

    fun changePassword(
        loginId: String,
        newPassword: String,
        birthDate: String,
    ) {
        userService.updatePassword(
            loginId = loginId,
            newRawPassword = newPassword,
            birthDate = birthDate,
        )
    }
}
