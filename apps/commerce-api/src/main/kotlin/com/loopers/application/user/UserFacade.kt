package com.loopers.application.user

import com.loopers.domain.user.UserService
import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.domain.user.dto.UserInfo
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(command: SignUpCommand) = userService.signUp(command)
    fun findUserInfo(id: Long): UserInfo = userService.findUserInfo(id)
    fun changePassword(id: Long, currentPassword: String, newPassword: String) {
        userService.changePassword(id, currentPassword, newPassword)
    }
}
