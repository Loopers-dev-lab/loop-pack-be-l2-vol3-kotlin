package com.loopers.application.event

import com.loopers.infrastructure.event.EventHandledEntity
import com.loopers.infrastructure.event.EventHandledJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

// [TODO-운영] event_handled 테이블은 Kafka retention(7일) 이후 데이터가 불필요.
// handledAt 기준 7일 초과 레코드를 batch delete하는 스케줄러 필요.
@Component
class IdempotencyService(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 이벤트 처리 선점을 시도한다.
     * INSERT 성공 시 true (이 인스턴스가 처리 책임), PK 중복 시 false (이미 처리됨).
     * check-then-insert 대신 insert-first로 race condition 방지.
     */
    fun tryMarkHandled(eventId: String, eventType: String): Boolean {
        return try {
            eventHandledJpaRepository.save(EventHandledEntity(eventId = eventId, eventType = eventType))
            true
        } catch (e: DataIntegrityViolationException) {
            log.debug("이미 처리된 이벤트입니다. eventId={}", eventId)
            false
        }
    }
}
