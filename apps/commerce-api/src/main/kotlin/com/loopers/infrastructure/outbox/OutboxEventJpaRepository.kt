package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, Long> {

    @Query(
        "SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC",
    )
    fun findUnpublished(pageable: Pageable): List<OutboxEvent>
}
