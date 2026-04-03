package com.loopers.application.user.queue

import com.loopers.domain.queue.EntryTokenRepository
import org.springframework.stereotype.Service

@Service
class EntryTokenValidationUseCase(
    private val entryTokenRepository: EntryTokenRepository,
) {
    fun validate(
        userId: Long,
        token: String,
    ): Boolean = entryTokenRepository.validate(userId, token)

    fun validateAndConsume(
        userId: Long,
        token: String,
    ): Boolean = entryTokenRepository.validateAndConsume(userId, token)
}
