package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventModel
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventJpaRepository : JpaRepository<OutboxEventModel, Long> {
    fun findTop100ByPublishedAtIsNullOrderByIdAsc(): List<OutboxEventModel>
}
