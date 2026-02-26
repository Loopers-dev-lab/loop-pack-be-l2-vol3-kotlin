package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime
import java.util.Optional

interface OrderJpaRepository : JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = ["_orderItems"])
    override fun findById(id: Long): Optional<Order>

    @EntityGraph(attributePaths = ["_orderItems"])
    fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Order>

    @EntityGraph(attributePaths = ["_orderItems"])
    override fun findAll(pageable: Pageable): Page<Order>

    @EntityGraph(attributePaths = ["_orderItems"])
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdWithItems(id: Long): Order?
}
