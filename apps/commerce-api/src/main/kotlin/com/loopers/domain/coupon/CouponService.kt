package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponService(
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun create(coupon: CouponModel): CouponModel {
        return couponRepository.save(coupon)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): CouponModel {
        return couponRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다: $id")
    }

    @Transactional(readOnly = true)
    fun findAllByIds(ids: List<Long>): List<CouponModel> {
        return couponRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<CouponModel> {
        return couponRepository.findAllByDeletedAtIsNull(pageable)
    }

    @Transactional
    fun delete(id: Long) {
        val coupon = findById(id)
        coupon.delete()
        couponRepository.save(coupon)
    }
}
