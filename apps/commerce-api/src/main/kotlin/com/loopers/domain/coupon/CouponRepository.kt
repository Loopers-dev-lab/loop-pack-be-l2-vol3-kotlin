package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {
    fun save(coupon: CouponModel): CouponModel
    fun findByIdAndDeletedAtIsNull(id: Long): CouponModel?
    fun findByIdForUpdate(id: Long): CouponModel?
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<CouponModel>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponModel>
}
