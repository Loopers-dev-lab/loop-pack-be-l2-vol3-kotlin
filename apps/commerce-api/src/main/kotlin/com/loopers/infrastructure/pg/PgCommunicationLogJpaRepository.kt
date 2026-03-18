package com.loopers.infrastructure.pg

import com.loopers.domain.pg.PgCommunicationLog
import org.springframework.data.jpa.repository.JpaRepository

interface PgCommunicationLogJpaRepository : JpaRepository<PgCommunicationLog, Long>
