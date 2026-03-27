package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.AvroSchemaProvider
import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxEventStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Component
class OutboxRelayScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val outboxEventFetcher: OutboxEventFetcher,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
    private val avroSchemaProvider: AvroSchemaProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_RETRY = 5
        private const val SEND_TIMEOUT_SECONDS = 10L
        private const val STUCK_SENDING_THRESHOLD_MINUTES = 5
    }

    // [설계 결정] 다중 인스턴스 환경에서 ShedLock 대신 FOR UPDATE SKIP LOCKED 사용.
    // 리더 선출은 리더만 relay하므로 처리량이 1/N이지만,
    // SKIP LOCKED는 모든 인스턴스가 비중복으로 relay하므로 수평 확장 가능.
    @Scheduled(fixedDelay = 1000)
    fun relay() {
        val events = outboxEventFetcher.fetchAndMarkSending { outboxEventRepository.findPendingEventsForUpdate(100) }
        if (events.isEmpty()) return

        relayEvents(events)
        log.debug("Outbox relay 완료. count={}", events.size)
    }

    @Scheduled(fixedDelay = 30000)
    fun retryFailed() {
        val events = outboxEventFetcher.fetchAndMarkSending { outboxEventRepository.findFailedEventsForUpdate(50) }
        if (events.isEmpty()) return

        relayEvents(events)
        log.debug("Outbox retry 완료. count={}", events.size)
    }

    /**
     * SENDING 상태로 마킹 후 서버가 죽으면 해당 이벤트가 PENDING으로도 FAILED로도 돌아가지 않는다.
     * 5분 이상 SENDING에 머물러 있는 이벤트를 PENDING으로 되돌려서 다음 relay 주기에 재시도한다.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    fun recoverStuckSending() {
        val recovered = outboxEventRepository.recoverStuckSending(STUCK_SENDING_THRESHOLD_MINUTES)
        if (recovered > 0) {
            log.warn("SENDING 상태 복구 완료. recovered={}", recovered)
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun cleanupPublished() {
        val deleted = outboxEventRepository.deletePublishedBefore(72)
        if (deleted > 0) {
            log.info("Outbox 정리 완료. deleted={}", deleted)
        }
    }

    /**
     * 트랜잭션 밖에서 Kafka 전송 후, 결과에 따라 개별 트랜잭션으로 상태 업데이트.
     * Kafka send 대기 동안 DB 커넥션/잠금을 점유하지 않는다.
     */
    private fun relayEvents(events: List<OutboxEvent>) {
        val futures = events.map { event -> sendAsync(event) }

        for ((index, event) in events.withIndex()) {
            try {
                futures[index].get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                updateEventStatus(requireNotNull(event.persistenceId), OutboxEventStatus.PUBLISHED, event.retryCount)
            } catch (e: Exception) {
                val failed = event.markFailed()
                if (failed.isRetryExhausted(MAX_RETRY)) {
                    updateEventStatus(requireNotNull(event.persistenceId), OutboxEventStatus.DEAD, failed.retryCount)
                    log.error("Outbox 이벤트 DEAD 전환. eventId={}, retryCount={}", event.eventId, failed.retryCount)
                } else {
                    updateEventStatus(requireNotNull(event.persistenceId), OutboxEventStatus.FAILED, failed.retryCount)
                    log.warn("Outbox relay 실패. eventId={}, retryCount={}", event.eventId, failed.retryCount, e)
                }
            }
        }
    }

    @Transactional
    fun updateEventStatus(id: Long, status: OutboxEventStatus, retryCount: Int) {
        outboxEventRepository.updateStatusAndRetryCount(id, status, retryCount)
    }

    @Suppress("UNCHECKED_CAST")
    private fun sendAsync(event: OutboxEvent): CompletableFuture<Void> {
        val payloadMap = objectMapper.readValue(event.payload, Map::class.java) as Map<String, Any?>

        val record = avroSchemaProvider.toGenericRecord(event.topic, payloadMap)
        return kafkaTemplate.send(event.topic, event.partitionKey, record)
            .thenAccept { }
    }
}
