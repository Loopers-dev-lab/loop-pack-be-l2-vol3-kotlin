package com.loopers.application.user

import com.loopers.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(loginId: String, password: String): Long? {
        val user = userRepository.findByLoginId(loginId) ?: return null
        if (!user.verifyPassword(password)) return null
        return user.id
    }
}
