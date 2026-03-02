package com.loopers.application.user

import com.loopers.domain.user.Password
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun execute(command: UserCommand.ChangePassword) {
        val user = userRepository.findById(command.userId)
            ?: throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)

        if (!passwordEncoder.matches(command.currentPassword, user.password.value)) {
            throw CoreException(UserErrorCode.INVALID_CURRENT_PASSWORD)
        }

        if (passwordEncoder.matches(command.newPassword, user.password.value)) {
            throw CoreException(UserErrorCode.SAME_PASSWORD)
        }

        Password.validate(command.newPassword, user.birthDate)

        val newEncodedPassword = passwordEncoder.encode(command.newPassword)
        user.changePassword(newEncodedPassword)
    }
}
