package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional
    fun createCoupon(criteria: CreateCouponCriteria): CouponInfo {
        val coupon = couponRepository.save(
            Coupon(
                name = criteria.name,
                type = criteria.type,
                value = criteria.value,
                minOrderAmount = criteria.minOrderAmount,
                expiredAt = criteria.expiredAt,
            ),
        )
        return CouponInfo.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getCoupon(couponId: Long): Coupon {
        return couponRepository.findByIdAndDeletedAtIsNull(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun getCouponInfo(couponId: Long): CouponInfo {
        return CouponInfo.from(getCoupon(couponId))
    }

    @Transactional(readOnly = true)
    fun getAllCoupons(pageable: Pageable): Page<CouponInfo> {
        return couponRepository.findAll(pageable).map { CouponInfo.from(it) }
    }

    @Transactional
    fun updateCoupon(couponId: Long, criteria: UpdateCouponCriteria): CouponInfo {
        val coupon = getCoupon(couponId)
        coupon.update(
            name = criteria.name,
            value = criteria.value,
            minOrderAmount = criteria.minOrderAmount,
            expiredAt = criteria.expiredAt,
        )
        val savedCoupon = couponRepository.save(coupon)
        return CouponInfo.from(savedCoupon)
    }

    @Transactional
    fun deleteCoupon(couponId: Long) {
        val coupon = getCoupon(couponId)
        coupon.delete()
        couponRepository.save(coupon)
    }

    @Transactional
    fun issueCoupon(couponId: Long, userId: Long): IssuedCouponInfo {
        val coupon = getCoupon(couponId)

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰은 발급할 수 없습니다.")
        }

        if (issuedCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }

        val issuedCoupon = issuedCouponRepository.save(
            IssuedCoupon(couponId = couponId, userId = userId),
        )
        return IssuedCouponInfo.from(issuedCoupon)
    }

    @Transactional(readOnly = true)
    fun getMyCoupons(userId: Long): List<IssuedCouponInfo> {
        return issuedCouponRepository.findAllByUserId(userId).map { IssuedCouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getIssuedCoupons(couponId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        return issuedCouponRepository.findAllByCouponId(couponId, pageable).map { IssuedCouponInfo.from(it) }
    }

    @Transactional
    fun getIssuedCouponWithLock(issuedCouponId: Long): IssuedCoupon {
        return issuedCouponRepository.findByIdWithLock(issuedCouponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.")
    }
}
