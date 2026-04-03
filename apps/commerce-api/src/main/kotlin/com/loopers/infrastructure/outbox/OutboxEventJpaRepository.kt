package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long> {

    @Query(
        value = """
            SELECT * FROM outbox_event
            WHERE status = :#{#status.name()}
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun findByStatusForUpdate(status: OutboxEventStatus, limit: Int): List<OutboxEventEntity>

    // clearAutomatically 미설정: 현재 UPDATE 후 같은 트랜잭션에서 재조회 경로 없음.
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = 'SENDING' WHERE e.id IN :ids")
    fun markAsSending(ids: List<Long>)

    // clearAutomatically 미설정: 현재 UPDATE 후 같은 트랜잭션에서 재조회 경로 없음.
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status WHERE e.id = :id")
    fun updateStatus(id: Long, status: OutboxEventStatus)

    // clearAutomatically 미설정: 현재 UPDATE 후 같은 트랜잭션에서 재조회 경로 없음.
    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status, e.retryCount = :retryCount WHERE e.id = :id")
    fun updateStatusAndRetryCount(id: Long, status: OutboxEventStatus, retryCount: Int)

    @Modifying
    @Query(
        value = "DELETE FROM outbox_event WHERE status = 'PUBLISHED' AND created_at < DATE_SUB(NOW(), INTERVAL :hours HOUR)",
        nativeQuery = true,
    )
    fun deletePublishedBefore(hours: Int): Int

    @Modifying
    @Query(
        value = """
            UPDATE outbox_event SET status = 'PENDING'
            WHERE status = 'SENDING' AND updated_at < DATE_SUB(NOW(), INTERVAL :minutes MINUTE)
        """,
        nativeQuery = true,
    )
    fun recoverStuckSending(minutes: Int): Int

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status WHERE e.id IN :ids")
    fun batchUpdateStatus(ids: List<Long>, status: OutboxEventStatus)
}
