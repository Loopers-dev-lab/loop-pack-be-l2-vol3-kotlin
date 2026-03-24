package com.loopers.domain.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 이벤트 핸들링 테이블: 멱등성 제어용 (경량, 빠른 조회).
 * PK 한 컬럼으로 existsById만 호출한다.
 */
@Entity
@Table(name = "event_handled")
class EventHandled(
    @Id
    val eventId: String,

    @Column(nullable = false)
    val handledAt: Instant = Instant.now(),
)
