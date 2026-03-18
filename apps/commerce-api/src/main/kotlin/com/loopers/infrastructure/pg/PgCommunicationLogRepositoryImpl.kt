package com.loopers.infrastructure.pg

import com.loopers.domain.pg.PgCommunicationLog
import com.loopers.domain.pg.PgCommunicationLogRepository
import org.springframework.stereotype.Component

@Component
class PgCommunicationLogRepositoryImpl(
    private val jpaRepository: PgCommunicationLogJpaRepository,
) : PgCommunicationLogRepository {

    override fun save(log: PgCommunicationLog): PgCommunicationLog {
        return jpaRepository.save(log)
    }
}
