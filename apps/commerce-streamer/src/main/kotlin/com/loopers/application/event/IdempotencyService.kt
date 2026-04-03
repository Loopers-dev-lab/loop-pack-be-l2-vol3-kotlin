package com.loopers.application.event

import com.loopers.infrastructure.event.EventHandledEntity
import com.loopers.infrastructure.event.EventHandledJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class IdempotencyService(
    private val eventHandledJpaRepository: EventHandledJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RETENTION_DAYS = 7L
    }

    /**
     * 이벤트 처리 선점을 시도한다.
     * INSERT 성공 시 true (이 인스턴스가 처리 책임), PK 중복 시 false (이미 처리됨).
     * check-then-insert 대신 insert-first로 race condition 방지.
     *
     * 반드시 비즈니스 로직과 같은 @Transactional 안에서 호출해야 한다.
     * 별도 트랜잭션으로 분리되면 멱등 마킹만 커밋되고 비즈니스 로직이 실패한 경우
     * 재시도가 불가능한 상태가 된다.
     */
    fun tryMarkHandled(eventId: String, eventType: String): Boolean {
        return try {
            eventHandledJpaRepository.save(EventHandledEntity(eventId = eventId, eventType = eventType))
            eventHandledJpaRepository.flush()
            true
        } catch (e: DataIntegrityViolationException) {
            log.debug("이미 처리된 이벤트입니다. eventId={}", eventId)
            false
        }
    }

    /**
     * 멱등성 체크 + 비즈니스 로직을 하나의 트랜잭션으로 묶어 실행한다.
     * tryMarkHandled 성공 후 action이 실패하면 둘 다 롤백된다.
     */
    @Transactional
    fun executeWithIdempotency(eventId: String, eventType: String, action: () -> Unit): Boolean {
        if (!tryMarkHandled(eventId, eventType)) return false
        action()
        return true
    }

    /**
     * Kafka retention(7일) 이후에는 재전달이 발생하지 않으므로,
     * handledAt 기준 7일 초과 레코드를 정리한다.
     */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    fun cleanupExpiredRecords() {
        val cutoff = ZonedDateTime.now().minusDays(RETENTION_DAYS)
        val deleted = eventHandledJpaRepository.deleteByHandledAtBefore(cutoff)
        if (deleted > 0) {
            log.info("event_handled 정리 완료. deleted={}", deleted)
        }
    }
}
