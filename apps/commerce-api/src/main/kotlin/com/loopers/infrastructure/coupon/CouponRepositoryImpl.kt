package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {
    override fun findById(id: Long): CouponModel? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdWithLock(id: Long): CouponModel? {
        return couponJpaRepository.findByIdWithLock(id)
    }

    override fun findAll(pageable: Pageable): Slice<CouponModel> {
        return couponJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun save(coupon: CouponModel): CouponModel {
        return couponJpaRepository.save(coupon)
    }
}
