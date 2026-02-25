package com.loopers.application.user

import com.loopers.application.user.model.UserChangePasswordCommand
import com.loopers.application.user.model.UserSignUpCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserFacade(
    private val userService: UserService,
) {
    @Transactional
    fun signUp(command: UserSignUpCommand): UserSignUpInfo {
        val user = userService.register(command)
        return UserSignUpInfo(loginId = user.loginId)
    }

    @Transactional
    fun changePassword(loginId: String, headerPassword: String, command: UserChangePasswordCommand) {
        userService.changePassword(loginId, headerPassword, command.currentPassword, command.newPassword)
    }

    @Transactional(readOnly = true)
    fun getMe(loginId: String, password: String): UserMeInfo {
        val user = userService.findByCredentials(loginId, password)
        return UserMeInfo(
            loginId = user.loginId,
            name = user.maskedName,
            birthDate = user.birthDate,
            email = user.email,
        )
    }
}
