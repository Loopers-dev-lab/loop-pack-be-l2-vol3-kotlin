package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.outbox.OutboxEventRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Outbox 헬스체크 컨슈머.
 *
 * Kafka에 발행된 메시지를 별도 컨슈머 그룹으로 수신하여
 * outbox_event의 status를 PUBLISHED로 마킹한다.
 *
 * 역할: "Producer의 할 일이 끝났다"를 확인 (Producer 책임 완료)
 * 비즈니스 컨슈머 실패는 Consumer 측 메커니즘(manual ack, retry)이 담당.
 */
@Component
class OutboxHealthCheckConsumer(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(OutboxHealthCheckConsumer::class.java)
        private const val GROUP_ID = "outbox-health-check"
    }

    @KafkaListener(
        topics = ["order-events", "catalog-events", "coupon-issue-requests"],
        groupId = GROUP_ID,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    @Transactional
    fun handleMessage(
        records: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        records.forEach { record ->
            try {
                val eventId = extractEventId(record.value())
                if (eventId != null) {
                    val outbox = outboxEventRepository.findByEventId(eventId)
                    if (outbox != null) {
                        outbox.markPublished()
                        log.debug("Outbox PUBLISHED [eventId={}, topic={}]", eventId, record.topic())
                    }
                }
            } catch (e: Exception) {
                log.warn("헬스체크 처리 실패 [topic={}, offset={}]", record.topic(), record.offset(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    private fun extractEventId(payload: String): String? {
        return try {
            val node = objectMapper.readTree(payload)
            node.get("eventId")?.asText()
        } catch (e: Exception) {
            null
        }
    }
}
