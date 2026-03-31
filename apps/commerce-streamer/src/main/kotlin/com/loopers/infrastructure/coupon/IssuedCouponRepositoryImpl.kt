package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.stereotype.Repository

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val issuedCouponMapper: IssuedCouponMapper,
) : IssuedCouponRepository {
    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponMapper.toDomain(
            issuedCouponJpaRepository.saveAndFlush(issuedCouponMapper.toEntity(issuedCoupon)),
        )
    }

    override fun findById(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByIdAndDeletedAtIsNull(id)
            ?.let { issuedCouponMapper.toDomain(it) }
    }

    override fun findByCouponIdAndUserId(couponId: Long, userId: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByCouponIdAndUserIdAndDeletedAtIsNull(couponId, userId)
            ?.let { issuedCouponMapper.toDomain(it) }
    }
}
