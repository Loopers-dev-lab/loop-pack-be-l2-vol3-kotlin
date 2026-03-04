package com.loopers.application.user.auth

import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAuthenticateUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    @Transactional(readOnly = true)
    fun authenticate(loginId: String, password: String): User {
        val user = userRepository.findByLoginId(loginId) ?: throw CoreException(ErrorType.UNAUTHORIZED)
        if (!passwordHasher.matches(RawPassword(password), user.password)) throw CoreException(ErrorType.UNAUTHORIZED)
        return user
    }

    @Transactional(readOnly = true)
    fun authenticateAndGetId(loginId: String, password: String): Long {
        return authenticate(loginId, password).id!!
    }
}
