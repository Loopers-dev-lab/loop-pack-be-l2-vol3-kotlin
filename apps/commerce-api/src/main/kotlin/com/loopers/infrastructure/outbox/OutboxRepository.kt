package com.loopers.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxRepository : JpaRepository<OutboxEvent, Long> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC LIMIT :limit")
    fun findUnpublished(@Param("limit") limit: Int = 100): List<OutboxEvent>
}
