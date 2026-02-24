package com.loopers.application.user

import com.loopers.domain.user.repository.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetUserInfoUseCase(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): UserInfo {
        val user = userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        return UserInfo.from(user)
    }
}
