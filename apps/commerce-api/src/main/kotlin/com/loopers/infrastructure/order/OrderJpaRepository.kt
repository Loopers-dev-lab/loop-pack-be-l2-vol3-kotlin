package com.loopers.infrastructure.order

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {

    fun findByUserId(userId: Long): List<OrderEntity>

    @Query("SELECT o FROM OrderEntity o WHERE o.userId = :userId AND o.createdAt >= :startAt AND o.createdAt <= :endAt AND o.deletedAt IS NULL")
    fun findByUserIdAndCreatedAtBetween(userId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderEntity>

    @Query("SELECT o FROM OrderEntity o WHERE o.deletedAt IS NULL")
    fun findAllActive(pageable: Pageable): List<OrderEntity>
}

interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {

    fun findByOrderId(orderId: Long): List<OrderItemEntity>

    fun findByOrderIdIn(orderIds: List<Long>): List<OrderItemEntity>
}
