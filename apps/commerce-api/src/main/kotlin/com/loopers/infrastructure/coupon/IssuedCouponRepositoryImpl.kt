package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {
    override fun findById(id: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findByIdWithLock(id: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findByIdWithLock(id)
    }

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Slice<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByCouponIdAndDeletedAtIsNull(couponId, pageable)
    }

    override fun findAllByUserId(userId: Long, pageable: Pageable): Slice<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }

    override fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel {
        return issuedCouponJpaRepository.save(issuedCoupon)
    }
}
