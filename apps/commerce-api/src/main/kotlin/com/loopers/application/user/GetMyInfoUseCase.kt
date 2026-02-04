package com.loopers.application.user

import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyInfoUseCase(
    private val userRepository: UserRepository,
) {

    /**
     * Retrieves the profile information for the given user.
     *
     * @param userId The identifier of the user to retrieve.
     * @return The user's profile information as a `UserInfo`.
     * @throws CoreException if no user exists for the provided `userId` (ErrorType.NOT_FOUND).
     */
    fun execute(userId: Long): UserInfo {
        val user = userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        return UserInfo.from(user)
    }
}