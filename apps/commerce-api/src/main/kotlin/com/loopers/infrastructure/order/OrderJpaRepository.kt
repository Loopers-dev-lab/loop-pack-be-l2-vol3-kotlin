package com.loopers.infrastructure.order

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = ["orderItems"])
    override fun findById(id: Long): Optional<OrderEntity>

    @EntityGraph(attributePaths = ["orderItems"])
    fun findAllByMemberId(memberId: Long): List<OrderEntity>
}
