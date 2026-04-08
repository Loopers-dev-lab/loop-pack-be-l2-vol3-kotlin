package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueEntryState
import com.loopers.application.queue.QueueStrategyType
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface QueueEntryJpaRepository : JpaRepository<QueueEntryEntity, Long> {
    fun findFirstByStrategyTypeAndMemberIdAndStateInOrderByIdDesc(
        strategyType: QueueStrategyType,
        memberId: Long,
        states: Collection<QueueEntryState>,
    ): QueueEntryEntity?

    fun findFirstByStrategyTypeAndToken(strategyType: QueueStrategyType, token: String): QueueEntryEntity?

    @Query(
        """
        select count(q)
        from QueueEntryEntity q
        where q.strategyType = :strategyType
          and q.state = :state
          and q.queueOrder < :queueOrder
        """,
    )
    fun countAhead(
        @Param("strategyType") strategyType: QueueStrategyType,
        @Param("state") state: QueueEntryState,
        @Param("queueOrder") queueOrder: Long,
    ): Long

    fun countByStrategyTypeAndState(strategyType: QueueStrategyType, state: QueueEntryState): Long

    fun findByStrategyTypeAndStateOrderByQueueOrderAsc(
        strategyType: QueueStrategyType,
        state: QueueEntryState,
        pageable: Pageable,
    ): List<QueueEntryEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        value = """
            select *
            from queue_entries
            where strategy_type = :strategyType
              and state = :state
            order by queue_order asc
            limit :batchSize
            for update skip locked
        """,
        nativeQuery = true,
    )
    fun findWaitingForUpdate(
        @Param("strategyType") strategyType: String,
        @Param("state") state: String,
        @Param("batchSize") batchSize: Int,
    ): List<QueueEntryEntity>
}
