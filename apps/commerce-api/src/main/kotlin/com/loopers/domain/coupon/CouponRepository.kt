package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findAllByIds(ids: List<Long>): List<Coupon>
    fun findAll(pageable: Pageable): Page<Coupon>
    fun deleteById(id: Long)
}
