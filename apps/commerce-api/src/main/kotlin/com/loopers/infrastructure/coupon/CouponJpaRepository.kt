package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<CouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponModel c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findByIdWithLock(id: Long): CouponModel?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Slice<CouponModel>
}
