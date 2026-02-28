package com.loopers.infrastructure.order

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdForUpdate(id: Long): OrderEntity?

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdWithItems(id: Long): OrderEntity?

    @Query(
        "SELECT o.id FROM OrderEntity o " +
            "WHERE o.userId = :userId AND o.orderedAt >= :startAt AND o.orderedAt < :endAt " +
            "ORDER BY o.orderedAt DESC",
    )
    fun findIdsByUserIdAndOrderedDateRange(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<Long>

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id IN :ids ORDER BY o.orderedAt DESC")
    fun findAllWithItemsByIdIn(ids: List<Long>): List<OrderEntity>

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items ORDER BY o.orderedAt DESC")
    fun findAllWithItems(): List<OrderEntity>
}
