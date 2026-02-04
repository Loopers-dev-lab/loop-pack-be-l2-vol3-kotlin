package com.loopers.domain.example

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): User {
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID 입니다.")
        }

        val validatedPassword = Password.of(password, birthday)
        val encodedPassword = passwordEncoder.encode(validatedPassword.value)

        val user = User(
            loginId = loginId,
            password = encodedPassword,
            name = name,
            email = email,
            birthday = birthday,
        )
        return userRepository.save(user)
    }
}
