package com.loopers.domain.user

import com.loopers.application.user.model.UserSignUpCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    fun register(command: UserSignUpCommand): User {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }

        val user = User.register(
            loginId = command.loginId,
            rawPassword = command.password,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
            passwordHasher = passwordHasher,
        )

        return userRepository.save(user)
    }

    fun changePassword(loginId: String, headerPassword: String, currentPassword: String, newPassword: String) {
        val user = findByCredentials(loginId, headerPassword)
        val updatedUser = user.changePassword(currentPassword, newPassword, passwordHasher)
        userRepository.save(updatedUser)
    }

    fun findByCredentials(loginId: String, password: String): User {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (!passwordHasher.matches(password, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED)
        }

        return user
    }
}
