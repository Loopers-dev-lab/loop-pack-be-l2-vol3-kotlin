package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserService(
    private val userRepository: UserRepository,
) {

    fun signUp(command: UserCommand.SignUp): User {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
        return userRepository.save(command.toEntity())
    }
}
