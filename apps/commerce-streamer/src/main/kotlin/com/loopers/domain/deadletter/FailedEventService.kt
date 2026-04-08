package com.loopers.domain.deadletter

import com.loopers.infrastructure.deadletter.FailedEventJpaRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FailedEventService(
    private val failedEventJpaRepository: FailedEventJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun save(record: ConsumerRecord<String, String>, error: Exception) {
        val eventId = record.headers().lastHeader("outbox-event-id")
            ?.value()?.let { String(it).toLong() }
        val eventType = record.headers().lastHeader("outbox-event-type")
            ?.value()?.let { String(it) }

        failedEventJpaRepository.save(
            FailedEvent(
                topic = record.topic(),
                eventKey = record.key(),
                payload = record.value() ?: "",
                errorMessage = "${error.javaClass.simpleName}: ${error.message}",
                eventId = eventId,
                eventType = eventType,
            ),
        )
        log.warn(
            "[DeadLetter] Saved failed event: topic={} key={} eventId={} error={}",
            record.topic(),
            record.key(),
            eventId,
            error.message,
        )
    }

    @Transactional(readOnly = true)
    fun findPendingEvents(): List<FailedEvent> {
        return failedEventJpaRepository.findTop100ByStatusOrderByCreatedAtAsc(FailedEventStatus.PENDING)
    }
}
