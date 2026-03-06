package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCouponModel
import com.loopers.domain.coupon.UserCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserCouponRepositoryImpl(
    private val jpa: UserCouponJpaRepository,
) : UserCouponRepository {
    override fun save(userCoupon: UserCouponModel): UserCouponModel = jpa.save(userCoupon)
    override fun findById(id: Long): UserCouponModel? = jpa.findById(id).orElse(null)
    override fun findAllByUserId(userId: Long): List<UserCouponModel> = jpa.findAllByUserId(userId)
    override fun findByIdAndUserId(id: Long, userId: Long): UserCouponModel? = jpa.findByIdAndUserId(id, userId)
    override fun findAllByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<UserCouponModel> =
        jpa.findAllByCouponTemplateId(couponTemplateId, pageable)
    override fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean =
        jpa.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)
}
