package com.loopers.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long> {
    fun findTop100ByPublishedAtIsNullOrderByIdAsc(): List<OutboxEventEntity>
}
