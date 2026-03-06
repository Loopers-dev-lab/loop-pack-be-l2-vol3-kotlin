package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserCouponJpaRepository : JpaRepository<UserCouponModel, Long> {
    fun findAllByUserId(userId: Long): List<UserCouponModel>
    fun findByIdAndUserId(id: Long, userId: Long): UserCouponModel?
    fun findAllByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<UserCouponModel>
    fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean
}
