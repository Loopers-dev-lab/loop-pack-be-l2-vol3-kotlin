package com.loopers.domain.coupon

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface CouponRepository {
    fun findById(id: Long): CouponModel?
    fun findByIdWithLock(id: Long): CouponModel?
    fun findAll(pageable: Pageable): Slice<CouponModel>
    fun save(coupon: CouponModel): CouponModel
}
