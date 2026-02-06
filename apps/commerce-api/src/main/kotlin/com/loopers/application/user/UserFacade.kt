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
}
