package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class AuthenticateUserUseCase(
    private val userService: UserService,
) {
    fun execute(loginId: String, password: String): Long? {
        val user = userService.getUserInfo(loginId) ?: return null
        if (!user.verifyPassword(password)) return null
        return user.id
    }
}
