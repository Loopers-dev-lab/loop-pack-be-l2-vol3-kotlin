package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class IssuedCouponReader(
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    fun getById(id: Long): IssuedCoupon {
        return issuedCouponRepository.findById(id)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)
    }

    fun getByIdForUpdate(id: Long): IssuedCoupon {
        return issuedCouponRepository.findByIdForUpdate(id)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)
    }

    fun getAllByMemberId(memberId: Long): List<IssuedCoupon> {
        return issuedCouponRepository.findAllByMemberId(memberId)
    }

    fun getAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon> {
        return issuedCouponRepository.findAllByCouponId(couponId, pageable)
    }
}
