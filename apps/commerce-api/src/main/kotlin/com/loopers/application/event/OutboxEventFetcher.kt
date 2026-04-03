package com.loopers.application.event

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * OutboxRelayScheduler에서 분리된 트랜잭션 컴포넌트.
 * self-invocation으로 @Transactional이 무효화되는 문제를 방지한다.
 */
@Component
class OutboxEventFetcher(
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun fetchAndMarkSending(fetcher: () -> List<OutboxEvent>): List<OutboxEvent> {
        val events = fetcher()
        if (events.isNotEmpty()) {
            val ids = events.map { requireNotNull(it.persistenceId) }
            outboxEventRepository.markAsSending(ids)
        }
        return events
    }
}
