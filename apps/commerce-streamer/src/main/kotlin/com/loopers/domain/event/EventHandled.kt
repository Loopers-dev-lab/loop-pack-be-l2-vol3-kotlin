package com.loopers.domain.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 이벤트 핸들링 테이블: 멱등성 제어용 (경량, 빠른 조회).
 * EventHandledJpaRepository.insertIgnore()로 원자적 INSERT를 수행하여
 * 중복 eventId는 무시하고, 반환값(0/1)으로 멱등성을 판단한다.
 */
@Entity
@Table(name = "event_handled")
class EventHandled(
    @Id
    val eventId: String,

    @Column(nullable = false)
    val handledAt: Instant = Instant.now(),
)
