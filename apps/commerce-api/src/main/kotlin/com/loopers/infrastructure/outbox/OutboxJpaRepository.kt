package com.loopers.infrastructure.outbox

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxJpaRepository : JpaRepository<OutboxEvent, Long> {
    fun findByPublishedAtIsNull(pageable: Pageable): List<OutboxEvent>
}
