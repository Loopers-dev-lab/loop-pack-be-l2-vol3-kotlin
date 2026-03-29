package com.loopers.domain.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.ZonedDateTime

/**
 * 이벤트 처리 이력 — 멱등성 보장.
 *
 * event_id를 PK로 사용하여, 동일 이벤트가 중복 처리되지 않도록 한다.
 * 오프셋 리셋이나 리밸런싱으로 인한 재처리 시 이미 처리된 이벤트를 skip한다.
 */
@Entity
@Table(name = "event_handled")
@Comment("이벤트 처리 이력 (멱등)")
class EventHandled(
    eventId: String,
) {

    @Id
    @Comment("도메인 이벤트 고유 ID")
    @Column(name = "event_id", nullable = false)
    var eventId: String = eventId
        protected set

    @Comment("처리 시점")
    @Column(name = "handled_at", nullable = false)
    var handledAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}
