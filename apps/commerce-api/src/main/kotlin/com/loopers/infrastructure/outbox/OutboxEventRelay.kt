package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.domain.outbox.OutboxStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

/**
 * Outbox 릴레이 — PENDING 이벤트를 Kafka로 발행한다.
 *
 * 폴링 스케줄러 (5초 간격)로 PENDING 이벤트를 조회하여 Kafka에 비동기 발행.
 * 발행 후 상태를 바로 PUBLISHED로 바꾸지 않는다.
 * → 헬스체크 컨슈머가 Kafka에서 메시지를 확인한 뒤 PUBLISHED로 마킹 (Producer 책임 확인)
 */
@Component
class OutboxEventRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    companion object {
        private val log = LoggerFactory.getLogger(OutboxEventRelay::class.java)
        private const val PENDING_TIMEOUT_MINUTES = 5L
    }

    /**
     * 폴링 기반 릴레이.
     *
     * PENDING 상태인 이벤트를 조회하여 Kafka로 병렬 발행한다.
     * - 비동기 콜백: 개별 발행의 성공/실패를 논블로킹으로 처리
     * - 실패한 건은 PENDING 상태 유지 → 다음 폴링에서 재시도 (At Least Once)
     * - allOf로 전체 발행 완료를 대기하여 스케줄러 중복 실행 방지
     */
    @Scheduled(fixedDelay = 1_000)
    @Transactional
    fun relayPendingEvents() {
        val pendingEvents = outboxEventRepository.findAllByStatus(OutboxStatus.PENDING)
        if (pendingEvents.isEmpty()) return

        log.info("Outbox 릴레이 시작 — {}건", pendingEvents.size)

        val futures = pendingEvents.map { event ->
            kafkaTemplate.send(
                event.topic,
                event.partitionKey,
                event.payload,
            ).whenComplete { result, ex ->
                if (ex != null) {
                    log.warn(
                        "Kafka 발행 실패 — 다음 폴링에서 재시도 [eventId={}, topic={}]",
                        event.eventId,
                        event.topic,
                        ex,
                    )
                } else {
                    log.debug(
                        "Kafka 발행 완료 [eventId={}, topic={}, partition={}, offset={}]",
                        event.eventId,
                        event.topic,
                        result.recordMetadata.partition(),
                        result.recordMetadata.offset(),
                    )
                }
            }
        }

        // 전체 발행 완료 대기 — 스케줄러 다음 실행 전에 현재 배치가 끝나도록
        CompletableFuture.allOf(*futures.map { it.toCompletableFuture() }.toTypedArray()).join()
    }

    /**
     * PENDING 타임아웃 처리.
     *
     * 5분 이상 PENDING 상태인 이벤트는 발행 실패로 간주하고 FAILED로 마킹한다.
     * - 릴레이가 발행했지만 HealthCheckConsumer가 확인하지 못한 경우
     * - Kafka 브로커 장애로 발행 자체가 안 된 경우
     * 운영자가 FAILED 건을 확인하여 수동 재처리하거나, 원인을 조사한다.
     */
    @Scheduled(fixedDelay = 60_000) // 1분 간격
    @Transactional
    fun markTimedOutEvents() {
        val threshold = ZonedDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES)
        val timedOutEvents = outboxEventRepository.findAllByStatusAndCreatedAtBefore(
            OutboxStatus.PENDING,
            threshold,
        )
        if (timedOutEvents.isEmpty()) return

        timedOutEvents.forEach { event ->
            event.markFailed()
            log.warn(
                "Outbox 타임아웃 → FAILED [eventId={}, topic={}, createdAt={}]",
                event.eventId,
                event.topic,
                event.createdAt,
            )
        }
        log.warn("Outbox 타임아웃 처리 — {}건 FAILED", timedOutEvents.size)
    }
}
