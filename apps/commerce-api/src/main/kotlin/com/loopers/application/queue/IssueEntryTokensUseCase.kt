package com.loopers.application.queue

import com.loopers.domain.queue.QueueTokenBatchProcessor
import org.springframework.stereotype.Component

@Component
class IssueEntryTokensUseCase(
    private val queueTokenBatchProcessor: QueueTokenBatchProcessor,
    private val queueProperties: QueueProperties,
) {

    fun execute(): List<IssuedTokenInfo> {
        return queueTokenBatchProcessor.popAndIssueTokens(
            count = queueProperties.batchSize,
            ttlSeconds = queueProperties.tokenTtlSeconds,
        ).map { entryToken ->
            IssuedTokenInfo(userId = entryToken.userId.value, token = entryToken.token)
        }
    }
}
