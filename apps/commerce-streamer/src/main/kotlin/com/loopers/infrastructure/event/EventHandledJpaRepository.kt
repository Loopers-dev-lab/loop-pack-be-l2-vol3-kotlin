package com.loopers.infrastructure.event

import com.loopers.domain.event.EventHandled
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventHandledJpaRepository : JpaRepository<EventHandled, String> {

    /**
     * 원자적 멱등성 체크: INSERT IGNORE로 중복 PK는 무시한다.
     * @return 1이면 신규 삽입(처리 진행), 0이면 이미 처리된 이벤트(스킵)
     */
    @Modifying
    @Query(
        value = "INSERT IGNORE INTO event_handled (event_id, handled_at) VALUES (:eventId, NOW())",
        nativeQuery = true,
    )
    fun insertIgnore(@Param("eventId") eventId: String): Int
}
