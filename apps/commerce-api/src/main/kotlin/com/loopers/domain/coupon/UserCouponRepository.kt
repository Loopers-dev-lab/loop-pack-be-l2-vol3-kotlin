package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserCouponRepository {
    fun save(userCoupon: UserCouponModel): UserCouponModel
    fun findById(id: Long): UserCouponModel?
    fun findAllByUserId(userId: Long): List<UserCouponModel>
    fun findByIdAndUserId(id: Long, userId: Long): UserCouponModel?
    fun findAllByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<UserCouponModel>
    fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean
}
