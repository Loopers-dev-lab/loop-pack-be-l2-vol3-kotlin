package com.loopers.infrastructure.consumer

import org.springframework.data.jpa.repository.JpaRepository

interface EventHandledJpaRepository : JpaRepository<EventHandledEntity, Long>
