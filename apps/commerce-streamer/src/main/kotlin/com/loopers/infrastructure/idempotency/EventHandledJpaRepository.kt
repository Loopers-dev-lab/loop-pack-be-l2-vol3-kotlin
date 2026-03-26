package com.loopers.infrastructure.idempotency

import com.loopers.domain.idempotency.EventHandled
import org.springframework.data.jpa.repository.JpaRepository

interface EventHandledJpaRepository : JpaRepository<EventHandled, String>
