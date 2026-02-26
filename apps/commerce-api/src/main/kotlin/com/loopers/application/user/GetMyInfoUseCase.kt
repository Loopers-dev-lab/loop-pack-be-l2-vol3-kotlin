package com.loopers.application.user

import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyInfoUseCase(
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long): UserInfo {
        val user = userRepository.findById(userId)
            ?: throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)
        return UserInfo.from(user)
    }
}
