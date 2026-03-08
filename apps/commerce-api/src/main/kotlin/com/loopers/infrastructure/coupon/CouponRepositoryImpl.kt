package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun findById(id: Long): Coupon? = couponJpaRepository.findById(id)
        .orElse(null)

    override fun findByUserIdAndTemplateId(userId: Long, templateId: Long): Coupon? =
        couponJpaRepository.findByUserIdAndTemplateId(userId, templateId)

    override fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): Coupon? =
        couponJpaRepository.findByUserIdAndTemplateIdForUpdate(userId, templateId)

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Coupon> =
        couponJpaRepository.findByUserId(userId, pageable)

    override fun findByUserIdAndStatus(
        userId: Long,
        status: CouponStatus,
        pageable: Pageable,
    ): Page<Coupon> = couponJpaRepository.findByUserIdAndStatus(userId, status, pageable)

    override fun findByTemplateId(templateId: Long, pageable: Pageable): Page<Coupon> =
        couponJpaRepository.findByTemplateId(templateId, pageable)

    override fun save(coupon: Coupon): Coupon = couponJpaRepository.save(coupon)

    override fun updateStatusToUsed(couponId: Long): Int = couponJpaRepository.updateStatusToUsed(couponId)
}
