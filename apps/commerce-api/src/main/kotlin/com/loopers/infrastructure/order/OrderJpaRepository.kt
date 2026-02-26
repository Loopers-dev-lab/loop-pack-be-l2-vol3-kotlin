package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdOrNull(@Param("id") id: Long): Order?

    @Query(
        "SELECT o FROM Order o WHERE o.userId = :userId " +
            "AND o.orderedAt >= :startDate AND o.orderedAt < :endDate " +
            "ORDER BY o.orderedAt DESC",
    )
    fun findByUserIdAndDateRange(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: ZonedDateTime,
        @Param("endDate") endDate: ZonedDateTime,
    ): List<Order>
}
