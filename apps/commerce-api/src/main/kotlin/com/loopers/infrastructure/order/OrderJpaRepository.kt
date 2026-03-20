package com.loopers.infrastructure.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderJpaModel, Long> {
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
