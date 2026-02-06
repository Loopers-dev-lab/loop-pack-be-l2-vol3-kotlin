package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(command: UserService.SignUpCommand): UserInfo {
        return userService.signUp(command)
            .let { UserInfo.from(it) }
    }

    fun getMe(loginId: String, loginPw: String): UserInfo {
        return userService.authenticate(loginId, loginPw)
            .let { UserInfo.from(it) }
    }

    fun changePassword(loginId: String, loginPw: String, command: UserService.ChangePasswordCommand) {
        val user = userService.authenticate(loginId, loginPw)
        userService.changePassword(user, command)
    }
}
