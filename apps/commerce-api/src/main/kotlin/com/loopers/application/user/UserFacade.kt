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

    fun getMe(loginId: String, loginPw: String): UserResult {
        return userService.authenticate(loginId, loginPw)
            .let { UserInfo.from(it) }
            .let { UserResult.from(it) }
    }

    fun changePassword(loginId: String, loginPw: String, criteria: ChangePasswordCriteria) {
        val command = ChangePasswordCommand(
            newPassword = criteria.newPassword,
        )
        userService.changePassword(loginId, loginPw, command)
    }
}
