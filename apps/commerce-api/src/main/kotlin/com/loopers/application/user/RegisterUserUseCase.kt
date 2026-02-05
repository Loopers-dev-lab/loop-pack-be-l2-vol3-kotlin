package com.loopers.application.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.GenderType
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncoder
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(command: RegisterUserCommand): Long {
        val loginId = LoginId(command.loginId)

        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 사용 중인 로그인 ID입니다: ${loginId.value}")
        }

        val user = User.register(
            loginId = loginId,
            rawPassword = command.password,
            name = Name(command.name),
            birthDate = BirthDate.from(command.birthDate),
            email = Email(command.email),
            gender = GenderType.valueOf(command.gender),
            encoder = passwordEncoder,
        )

        return userRepository.save(user)
    }
}
