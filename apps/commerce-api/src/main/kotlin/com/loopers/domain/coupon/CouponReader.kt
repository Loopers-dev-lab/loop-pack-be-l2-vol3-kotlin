package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponReader(
    private val couponRepository: CouponRepository,
) {

    fun getById(id: Long): Coupon {
        return couponRepository.findById(id)
            ?: throw CoreException(ErrorType.COUPON_NOT_FOUND)
    }

    fun getAllByIds(ids: List<Long>): List<Coupon> {
        return couponRepository.findAllByIds(ids)
    }

    fun getAll(pageable: Pageable): Page<Coupon> {
        return couponRepository.findAll(pageable)
    }
}
