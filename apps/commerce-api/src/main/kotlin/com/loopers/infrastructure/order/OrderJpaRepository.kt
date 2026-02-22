package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    @Query(
        """
        SELECT o FROM OrderModel o LEFT JOIN FETCH o.orderItems
        WHERE o.memberId = :memberId AND o.orderedAt >= :startAt
        AND o.orderedAt < :endAt ORDER BY o.orderedAt DESC
        """,
    )
    fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderModel>
}
