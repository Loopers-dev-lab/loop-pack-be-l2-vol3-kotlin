package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findByIds(ids: List<Long>): List<Coupon>
    fun findAll(pageable: Pageable): Page<Coupon>

    /**
     * 발급 수량을 원자적으로 증가시킨다.
     * @return 영향받은 행 수 (0이면 수량 초과)
     */
    fun incrementIssuedCount(couponId: Long): Int
}
