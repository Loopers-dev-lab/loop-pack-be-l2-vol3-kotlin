package com.loopers.application.user

import com.loopers.domain.user.Password
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun execute(command: UserCommand.Register): UserInfo {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(UserErrorCode.DUPLICATE_LOGIN_ID)
        }

        Password.validate(command.rawPassword, command.birthDate)

        val encodedPassword = passwordEncoder.encode(command.rawPassword)
        val user = User.create(
            loginId = command.loginId,
            encodedPassword = encodedPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )
        val saved = userRepository.save(user)
        return UserInfo.from(saved)
    }
}
