package com.loopers.infrastructure.order

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = ["orderItems"])
    override fun findById(id: Long): Optional<OrderEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = ["orderItems"])
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    fun findByIdForUpdate(id: Long): OrderEntity?

    @EntityGraph(attributePaths = ["orderItems"])
    fun findAllByMemberId(memberId: Long): List<OrderEntity>
}
