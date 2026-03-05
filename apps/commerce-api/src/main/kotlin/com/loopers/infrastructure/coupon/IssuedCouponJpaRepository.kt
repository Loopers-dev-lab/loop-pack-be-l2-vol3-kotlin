package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCouponModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): IssuedCouponModel?

    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): IssuedCouponModel?

    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Slice<IssuedCouponModel>

    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Slice<IssuedCouponModel>
}
