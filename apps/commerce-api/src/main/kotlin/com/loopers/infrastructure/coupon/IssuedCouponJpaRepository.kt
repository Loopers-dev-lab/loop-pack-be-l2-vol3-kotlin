package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCouponModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCouponModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ic FROM IssuedCouponModel ic WHERE ic.id = :id AND ic.deletedAt IS NULL")
    fun findByIdWithLock(id: Long): IssuedCouponModel?

    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): IssuedCouponModel?

    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Slice<IssuedCouponModel>

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Slice<IssuedCouponModel>
}
