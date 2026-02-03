package com.loopers.domain.user

import org.springframework.stereotype.Component

@Component
class UserService(
    private val userRepository: UserRepository,
) {

    fun signUp(command: UserCommand.SignUp): User {
        return userRepository.save(command.toEntity())
    }
}
