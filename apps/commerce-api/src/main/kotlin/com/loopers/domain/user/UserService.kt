package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    fun register(loginId: String, rawPassword: String, name: String, birthDate: LocalDate, email: String): User {
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }

        val user = User.register(
            loginId = loginId,
            rawPassword = rawPassword,
            name = name,
            birthDate = birthDate,
            email = email,
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
