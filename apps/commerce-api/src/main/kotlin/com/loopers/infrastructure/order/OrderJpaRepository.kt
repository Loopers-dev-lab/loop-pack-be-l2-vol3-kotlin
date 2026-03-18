package com.loopers.infrastructure.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): OrderEntity?
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
