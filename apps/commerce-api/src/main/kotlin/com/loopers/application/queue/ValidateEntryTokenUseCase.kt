package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ValidateEntryTokenUseCase(
    private val entryTokenRepository: EntryTokenRepository,
) {

    fun execute(userId: Long, token: String) {
        val storedToken = entryTokenRepository.find(UserId(userId))
            ?: throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 만료되었거나 유효하지 않습니다.")

        if (token != storedToken) {
            throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 유효하지 않습니다.")
        }

        entryTokenRepository.delete(UserId(userId))
    }
}
