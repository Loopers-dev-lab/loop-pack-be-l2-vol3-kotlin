package com.loopers.application.user

import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun registerUser(loginId: String, password: String, name: String, birth: String, email: String): UserInfo {
        return userService.registerUser(loginId, password, name, birth, email)
            .let { UserInfo.from(it) }
    }

    fun getUser(loginId: String, password: String): UserInfo {
        return userService.getUserByLoginIdAndPassword(loginId, password)
            ?.let { UserInfo.from(it) }
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
    }

    fun changePassword(loginId: String, oldPassword: String, newPassword: String): UserInfo {
        return userService.chagePassword(loginId, oldPassword, newPassword)
            ?.let { UserInfo.from(it) }
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
    }
}
