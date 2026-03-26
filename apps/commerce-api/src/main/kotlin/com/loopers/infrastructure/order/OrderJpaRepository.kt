package com.loopers.infrastructure.order

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.deletedAt IS NULL")
    fun findByIdForUpdate(id: Long): OrderEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.userId = :userId AND o.deletedAt IS NULL")
    fun findByIdAndUserIdForUpdate(id: Long, userId: Long): OrderEntity?
    fun findByIdAndUserIdAndDeletedAtIsNull(id: Long, userId: Long): OrderEntity?
    fun findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
        userId: Long,
        from: ZonedDateTime,
        toExclusive: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
    fun findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
        from: ZonedDateTime,
        toExclusive: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderEntity>
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
