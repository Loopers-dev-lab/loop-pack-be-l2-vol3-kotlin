package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderJpaRepository : JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdForUpdate(id: Long): Order?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.status = com.loopers.domain.order.OrderStatus.PENDING")
    fun findByIdForUpdateWithPending(id: Long): Order?
}
