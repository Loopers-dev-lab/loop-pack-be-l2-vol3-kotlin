package com.loopers.application.user

import com.loopers.application.user.model.UserSignUpCommand
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserSignUpUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
) {
    @Transactional
    fun signUp(command: UserSignUpCommand): UserSignUpResult {
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
        val saved = userRepository.save(user)
        return UserSignUpResult(loginId = saved.loginId.value)
    }
}
