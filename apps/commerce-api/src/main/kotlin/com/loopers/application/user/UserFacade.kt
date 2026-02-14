package com.loopers.application.user

import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(command: UserCommand.SignUp): UserInfo {
        return userService.signUp(command)
            .let { UserInfo.from(it) }
    }

    fun getUserInfo(userId: Long): UserInfo {
        return UserInfo.fromWithMaskedName(userService.getUser(userId))
    }

    fun changePassword(userId: Long, command: UserCommand.ChangePassword) {
        userService.changePassword(userId, command)
    }
}
