package com.loopers.infrastructure.order

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdForUpdate(id: Long): OrderEntity?

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdWithItems(id: Long): OrderEntity?

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.orderedAt DESC")
    fun findAllByUserIdWithItems(userId: Long): List<OrderEntity>

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items ORDER BY o.orderedAt DESC")
    fun findAllWithItems(): List<OrderEntity>
}
