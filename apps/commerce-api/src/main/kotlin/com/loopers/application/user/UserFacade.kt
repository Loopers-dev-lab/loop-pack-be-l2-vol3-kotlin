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

    fun getMe(userId: Long): UserInfo {
        return userService.getMe(userId)
            .let { UserInfo.from(it) }
    }
}
