package com.loopers.application.user

import com.loopers.domain.user.ChangePasswordCommand
import com.loopers.domain.user.SignUpCommand
import com.loopers.domain.user.UserInfo
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(criteria: SignUpCriteria): UserResult {
        val command = SignUpCommand(
            loginId = criteria.loginId,
            password = criteria.password,
            name = criteria.name,
            birthday = criteria.birthday,
            email = criteria.email,
        )
        return userService.signUp(command)
            .let { UserInfo.from(it) }
            .let { UserResult.from(it) }
    }

    fun getMe(userId: Long): UserResult {
        return userService.findById(userId)
            .let { UserInfo.from(it) }
            .let { UserResult.from(it) }
    }

    fun changePassword(userId: Long, criteria: ChangePasswordCriteria) {
        val command = ChangePasswordCommand(
            newPassword = criteria.newPassword,
        )
        userService.changePassword(userId, command)
    }
}
