package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface CouponJpaRepository : JpaRepository<CouponModel, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByIdAndDeletedAtIsNull(id: Long): CouponModel?
}
