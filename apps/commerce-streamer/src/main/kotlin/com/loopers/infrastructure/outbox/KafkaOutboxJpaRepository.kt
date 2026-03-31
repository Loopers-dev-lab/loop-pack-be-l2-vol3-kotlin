package com.loopers.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface KafkaOutboxJpaRepository : JpaRepository<KafkaOutboxEntity, Long> {
    fun findAllByPublishedAtIsNullOrderByIdAsc(): List<KafkaOutboxEntity>
}
