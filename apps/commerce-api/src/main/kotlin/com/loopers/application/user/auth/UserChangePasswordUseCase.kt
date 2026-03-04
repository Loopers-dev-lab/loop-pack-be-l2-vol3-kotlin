package com.loopers.application.user.auth

import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserChangePasswordUseCase(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    @Transactional
    fun changePassword(loginId: String, headerPassword: String, command: UserAuthCommand.ChangePassword) {
        val user = userAuthenticateUseCase.authenticate(loginId, headerPassword)
        val updatedUser = user.changePassword(command.currentPassword, command.newPassword, passwordHasher)
        userRepository.save(updatedUser)
    }
}
