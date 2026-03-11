package com.loopers.application.user

import com.loopers.domain.user.repository.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RegisterUserUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(loginId: String, password: String, name: String, birthDate: LocalDate, email: String): UserInfo {
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
        val command = UserCommand.SignUp(
            loginId = loginId,
            password = password,
            name = name,
            birthDate = birthDate,
            email = email,
        )
        val user = userRepository.save(command.toUser())
        return UserInfo.from(user)
    }
}
