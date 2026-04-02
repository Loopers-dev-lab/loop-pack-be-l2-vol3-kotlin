package com.loopers.application.queue

import com.loopers.domain.queue.token.model.EntryToken
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import org.springframework.stereotype.Component

@Component
class IssueEntryTokensUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueProperties: QueueProperties,
) {

    fun execute(): List<IssuedTokenInfo> {
        val poppedUsers = waitingQueueRepository.popMin(queueProperties.batchSize)
        if (poppedUsers.isEmpty()) return emptyList()

        return poppedUsers.map { userId ->
            val entryToken = EntryToken.issue(userId)
            entryTokenRepository.issue(userId, entryToken.token, queueProperties.tokenTtlSeconds)
            IssuedTokenInfo(userId = userId.value, token = entryToken.token)
        }
    }
}
