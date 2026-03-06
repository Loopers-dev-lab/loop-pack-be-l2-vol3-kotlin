package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponJpaRepository : JpaRepository<CouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponModel?
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<CouponModel>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponModel>
}
