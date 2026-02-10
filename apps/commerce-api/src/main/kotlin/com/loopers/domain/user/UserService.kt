package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun signUp(command: UserCommand.SignUp): User {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
        return userRepository.save(command.toEntity())
    }

    @Transactional(readOnly = true)
    fun getUserInfo(loginId: String): User? {
        return userRepository.findByLoginId(loginId)
    }

    @Transactional(readOnly = true)
    fun getUser(userId: Long): User {
        return userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
    }

    @Transactional
    fun changePassword(userId: Long, command: UserCommand.ChangePassword) {
        val user = userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        if (!user.verifyPassword(command.currentPassword)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.")
        }

        user.changePassword(command.newPassword)
    }
}
