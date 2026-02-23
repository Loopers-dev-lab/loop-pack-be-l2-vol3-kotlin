package com.loopers.application.user

import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class ChangePasswordUseCase(
    private val userService: UserService,
) {
    fun execute(userId: Long, currentPassword: String, newPassword: String) {
        val command = UserCommand.ChangePassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
        )
        userService.changePassword(userId, command)
    }
}
