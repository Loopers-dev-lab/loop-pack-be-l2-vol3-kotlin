package com.loopers.application.user

import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserMeService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    @Transactional(readOnly = true)
    fun getMe(loginId: String, password: String): UserMeInfo {
        val user = findByCredentials(loginId, password)
        return UserMeInfo(
            loginId = user.loginId.value,
            name = user.maskedName,
            birthDate = user.birthDate,
            email = user.email.value,
        )
    }

    private fun findByCredentials(loginId: String, password: String): User {
        val user = userRepository.findByLoginId(loginId) ?: throw CoreException(ErrorType.UNAUTHORIZED)
        if (!passwordHasher.matches(RawPassword(password), user.password)) throw CoreException(ErrorType.UNAUTHORIZED)
        return user
    }
}
