package com.loopers.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEventJpaModel, String> {
    fun findTop100ByPublishedAtIsNullOrderByCreatedAtAsc(): List<OutboxEventJpaModel>
}
