package com.loopers.user.application

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.user.application.model.UserSignUpCommand
import com.loopers.user.domain.User
import com.loopers.user.domain.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun signUp(command: UserSignUpCommand): UserInfo {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }

        val encodedPassword = passwordEncoder.encode(command.password)

        val user = User.register(
            loginId = command.loginId,
            password = encodedPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )

        val savedUser = userRepository.save(user)
        return UserInfo(loginId = savedUser.loginId)
    }
}
