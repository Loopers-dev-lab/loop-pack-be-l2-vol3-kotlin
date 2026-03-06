package com.loopers.infrastructure.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCouponEntity?

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long): List<IssuedCouponEntity>

    fun findAllByCouponIdAndDeletedAtIsNull(
        couponId: Long,
        pageable: Pageable,
    ): Page<IssuedCouponEntity>

    @Modifying
    @Query(
        """
        UPDATE IssuedCouponEntity e
           SET e.status = 'USED',
               e.usedAt = :usedAt,
               e.version = e.version + 1,
               e.updatedAt = :now
         WHERE e.id = :id
           AND e.version = :version
           AND e.status = 'AVAILABLE'
           AND e.deletedAt IS NULL
        """,
    )
    fun useOptimistic(
        @Param("id") id: Long,
        @Param("version") version: Long,
        @Param("usedAt") usedAt: ZonedDateTime,
        @Param("now") now: ZonedDateTime,
    ): Int
}
