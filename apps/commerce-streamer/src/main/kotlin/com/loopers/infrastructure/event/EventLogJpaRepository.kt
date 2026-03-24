package com.loopers.infrastructure.event

import com.loopers.domain.event.EventLog
import org.springframework.data.jpa.repository.JpaRepository

interface EventLogJpaRepository : JpaRepository<EventLog, Long>
