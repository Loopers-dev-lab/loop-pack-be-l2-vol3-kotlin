package com.loopers.application.user

import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(command: UserCommand.SignUp): UserInfo {
        return userService.signUp(command)
            .let { UserInfo.from(it) }
    }

    fun getUserInfo(loginId: String): UserInfo {
        return userService.getUserInfo(loginId)
            ?.let { UserInfo.fromWithMaskedName(it) }
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
    }

    fun changePassword(loginId: String, command: UserCommand.ChangePassword) {
        userService.changePassword(loginId, command)
    }
}
