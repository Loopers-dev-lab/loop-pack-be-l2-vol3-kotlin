package com.loopers.domain.pg

interface PgCommunicationLogRepository {
    fun save(log: PgCommunicationLog): PgCommunicationLog
}
