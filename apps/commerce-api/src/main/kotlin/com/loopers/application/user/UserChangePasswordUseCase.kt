package com.loopers.application.user

import com.loopers.application.user.model.UserChangePasswordCommand
import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    @Transactional
    fun changePassword(loginId: String, headerPassword: String, command: UserChangePasswordCommand) {
        val user = findByCredentials(loginId, headerPassword)
        val updatedUser = user.changePassword(command.currentPassword, command.newPassword, passwordHasher)
        userRepository.save(updatedUser)
    }

    private fun findByCredentials(loginId: String, password: String): User {
        val user = userRepository.findByLoginId(loginId) ?: throw CoreException(ErrorType.UNAUTHORIZED)
        if (!passwordHasher.matches(RawPassword(password), user.password)) throw CoreException(ErrorType.UNAUTHORIZED)
        return user
    }
}
