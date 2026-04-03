package com.loopers.infrastructure.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface EventHandledJpaRepository : JpaRepository<EventHandledEntity, String> {
    fun existsByEventId(eventId: String): Boolean

    @Modifying
    @Query("DELETE FROM EventHandledEntity e WHERE e.handledAt < :cutoff")
    fun deleteByHandledAtBefore(cutoff: ZonedDateTime): Int
}
