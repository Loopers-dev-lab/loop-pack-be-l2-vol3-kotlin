package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<Coupon, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Coupon?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findByIdWithLockAndDeletedAtIsNull(id: Long): Coupon?
}
