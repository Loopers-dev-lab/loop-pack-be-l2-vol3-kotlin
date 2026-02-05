package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun registerUser(loginId: String, password: String, name: String, birth: String, email: String): UserInfo {
        return userService.registerUser(loginId, password, name, birth, email)
            .let { UserInfo.from(it) }
    }
}
