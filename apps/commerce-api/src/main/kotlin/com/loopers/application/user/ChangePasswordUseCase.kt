package com.loopers.application.user

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.user.repository.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun execute(userId: Long, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(UserId(userId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        if (user.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        }
        user.changePassword(currentPassword, newPassword)
        userRepository.save(user)
    }
}
