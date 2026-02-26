package com.loopers.application.user

import com.loopers.domain.user.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun execute(loginId: String, password: String): Long? {
        val user = userRepository.findByLoginId(loginId) ?: return null
        if (user.isDeleted()) return null
        if (!user.verifyPassword(password)) return null
        return user.id.value
    }
}
