package com.loopers.infrastructure.order

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderJpaModel, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaModel o WHERE o.id = :id")
    fun findByIdWithLock(id: Long): OrderJpaModel?

    @Query(
        """
        SELECT o FROM OrderJpaModel o LEFT JOIN FETCH o.orderItems
        WHERE o.memberId = :memberId AND o.orderedAt >= :startAt
        AND o.orderedAt < :endAt ORDER BY o.orderedAt DESC
        """,
    )
    fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderJpaModel>
}
