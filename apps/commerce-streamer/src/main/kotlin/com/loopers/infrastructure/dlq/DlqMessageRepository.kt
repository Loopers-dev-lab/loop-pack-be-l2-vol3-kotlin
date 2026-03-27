package com.loopers.infrastructure.dlq

import com.loopers.domain.dlq.DlqMessage
import com.loopers.domain.dlq.DlqStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DlqMessageRepository : JpaRepository<DlqMessage, Long> {
    fun findByStatus(status: DlqStatus, pageable: Pageable): Page<DlqMessage>

    fun findByStatusAndRetryCountLessThan(
        status: DlqStatus,
        maxRetries: Int,
        pageable: Pageable,
    ): Page<DlqMessage>

    fun findByOriginalTopic(topic: String, pageable: Pageable): Page<DlqMessage>

    fun countByStatus(status: DlqStatus): Long
}
