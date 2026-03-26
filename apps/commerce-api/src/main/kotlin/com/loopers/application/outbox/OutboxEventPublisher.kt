package com.loopers.application.outbox

import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.domain.outbox.OutboxEventRepository
import org.springframework.stereotype.Component

@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
) {
    fun publish(aggregateType: String, aggregateId: Long, eventType: String, payload: String) {
        outboxEventRepository.save(
            OutboxEventModel(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
            ),
        )
    }
}
