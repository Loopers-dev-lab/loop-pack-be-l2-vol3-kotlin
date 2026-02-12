package com.loopers.application.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserService
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
            loginId = LoginId(loginId),
            rawPassword = Password(password),
            name = Name(name),
            birthDate = BirthDate(birthDate),
            email = Email(email),
        ).let { UserInfo.from(it) }
    }

    fun getUserInfo(loginId: LoginId): UserInfo {
        return userService.getUserByLoginId(loginId)
            .let { UserInfo.from(it) }
    }

    fun changePassword(
        loginId: LoginId,
        birthDate: BirthDate,
        newPassword: String,
    ) {
        userService.updatePassword(
            loginId = loginId,
            newRawPassword = Password(newPassword),
            birthDate = birthDate,
        )
    }
}
