package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<OrderModel>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderModel>

    @Query(
        "SELECT o FROM OrderModel o WHERE o.userId = :userId " +
            "AND o.deletedAt IS NULL " +
            "AND CAST(o.createdAt AS localdate) >= :startAt " +
            "AND CAST(o.createdAt AS localdate) <= :endAt",
    )
    fun findAllByUserIdAndCreatedAtBetween(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: LocalDate,
        @Param("endAt") endAt: LocalDate,
        pageable: Pageable,
    ): Page<OrderModel>
}
