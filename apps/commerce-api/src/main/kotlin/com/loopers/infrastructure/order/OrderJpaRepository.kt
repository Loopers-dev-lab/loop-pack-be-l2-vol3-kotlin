package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<Order, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Order?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Order>

    @Query(
        """
        SELECT o FROM Order o
        WHERE o.userId = :userId
          AND o.createdAt BETWEEN :startAt AND :endAt
          AND o.deletedAt IS NULL
        """,
    )
    fun findByUserIdAndCreatedAtBetween(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: ZonedDateTime,
        @Param("endAt") endAt: ZonedDateTime,
    ): List<Order>
}
