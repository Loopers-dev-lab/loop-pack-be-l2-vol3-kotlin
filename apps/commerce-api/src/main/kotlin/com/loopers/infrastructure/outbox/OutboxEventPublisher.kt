package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.outbox.OutboxPublisher
import org.springframework.stereotype.Component

/**
 * Outbox 이벤트 발행 구현체.
 *
 * 비즈니스 트랜잭션 안에서 호출하여 outbox_events 테이블에 INSERT한다.
 * Outbox Relay가 별도로 Kafka에 발행한다.
 */
@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : OutboxPublisher {

    override fun publish(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        version: Long,
        payload: Any,
    ) {
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                version = version,
                payload = objectMapper.writeValueAsString(payload),
            ),
        )
    }
}
