package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCouponModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCouponModel?

    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): IssuedCouponModel?

    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Slice<IssuedCouponModel>

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Slice<IssuedCouponModel>

    @Modifying
    @Query(
        "UPDATE IssuedCouponModel c SET c.status = 'USED', c.usedAt = :now, c.updatedAt = :now " +
            "WHERE c.id = :id AND c.status = 'AVAILABLE' AND c.deletedAt IS NULL",
    )
    fun updateStatusToUsed(
        @Param("id") id: Long,
        @Param("now") now: ZonedDateTime,
    ): Int
}
