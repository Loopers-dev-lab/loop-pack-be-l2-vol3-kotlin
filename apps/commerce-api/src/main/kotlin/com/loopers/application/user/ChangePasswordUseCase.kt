package com.loopers.application.user

import com.loopers.domain.user.PasswordEncoder
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun execute(userId: Long, command: ChangePasswordCommand) {
        val user = userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        val updatedUser = user.changePassword(
            oldPassword = command.oldPassword,
            newPassword = command.newPassword,
            encoder = passwordEncoder,
        )

        userRepository.save(updatedUser)
    }
}
