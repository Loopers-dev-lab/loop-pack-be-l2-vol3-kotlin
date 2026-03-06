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
        value = """
            UPDATE issued_coupon
            SET status = 'USED',
                used_at = :usedAt,
                version = version + 1,
                updated_at = :now
            WHERE id = :id
              AND version = :version
              AND status = 'AVAILABLE'
              AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun useOptimistic(
        @Param("id") id: Long,
        @Param("version") version: Long,
        @Param("usedAt") usedAt: ZonedDateTime,
        @Param("now") now: ZonedDateTime,
    ): Int
}
