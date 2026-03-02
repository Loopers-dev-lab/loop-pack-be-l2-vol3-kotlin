package com.loopers.application.user

import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun execute(loginId: String, rawPassword: String): Long {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)

        if (!passwordEncoder.matches(rawPassword, user.password.value)) {
            throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)
        }

        return requireNotNull(user.id) { "Authenticated user must have an id" }
    }
}
