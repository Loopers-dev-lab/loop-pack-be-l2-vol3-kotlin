package com.loopers.domain.outbox

import com.loopers.infrastructure.outbox.OutboxEvent
import com.loopers.infrastructure.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * 도메인 이벤트를 Outbox에 발행
     *
     * @param event 발행할 도메인 이벤트
     * @param aggregateId aggregate 식별자 (기본적으로 Kafka partition key로 사용)
     * @param topic Kafka 토픽
     * @param partitionKey Kafka partition key (동시성 제어용)
     *   - null: aggregateId를 key로 사용
     *   - "userId:templateId": 같은 사용자+템플릿 요청은 같은 파티션으로 라우팅
     */
    fun publish(
        event: Any,
        aggregateId: Long,
        topic: String = "metrics-events",
        partitionKey: String? = null,
    ) {
        val payload = objectMapper.writeValueAsString(event)
        val outboxEvent = OutboxEvent(
            aggregateId = aggregateId,
            eventType = event::class.simpleName!!,
            payload = payload,
            topic = topic,
            partitionKey = partitionKey,
        )
        outboxRepository.save(outboxEvent)
    }
}
