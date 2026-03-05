package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponService(
    private val couponTemplateRepository: CouponTemplateRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun getCouponTemplate(id: Long): CouponTemplate {
        return couponTemplateRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    fun getCouponTemplates(pageable: Pageable): Page<CouponTemplate> {
        return couponTemplateRepository.findAllByDeletedAtIsNull(pageable)
    }

    @Transactional
    fun createCouponTemplate(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplate {
        return couponTemplateRepository.save(
            CouponTemplate(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            ),
        )
    }

    @Transactional
    fun updateCouponTemplate(
        id: Long,
        name: String,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplate {
        val template = getCouponTemplate(id)
        template.update(name = name, value = value, minOrderAmount = minOrderAmount, expiredAt = expiredAt)
        return template
    }

    @Transactional
    fun deleteCouponTemplate(id: Long) {
        val template = getCouponTemplate(id)
        template.delete()
    }

    @Transactional
    fun issueCoupon(userId: Long, couponTemplateId: Long): IssuedCoupon {
        val template = getCouponTemplate(couponTemplateId)
        if (template.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        return issuedCouponRepository.save(IssuedCoupon(userId = userId, couponTemplateId = couponTemplateId))
    }

    fun getIssuedCoupon(id: Long): IssuedCoupon {
        return issuedCouponRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND)
    }

    fun getUserIssuedCoupons(userId: Long): List<IssuedCoupon> {
        return issuedCouponRepository.findAllByUserIdAndDeletedAtIsNull(userId)
    }

    fun getIssuedCouponsByCouponTemplateId(couponTemplateId: Long, pageable: Pageable): Page<IssuedCoupon> {
        return issuedCouponRepository.findAllByCouponTemplateIdAndDeletedAtIsNull(couponTemplateId, pageable)
    }
}
