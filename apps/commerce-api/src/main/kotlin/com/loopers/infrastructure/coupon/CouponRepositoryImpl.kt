package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: CouponModel): CouponModel {
        return couponJpaRepository.save(coupon)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): CouponModel? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<CouponModel> {
        return couponJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponModel> {
        return couponJpaRepository.findAllByDeletedAtIsNull(pageable)
    }
}
