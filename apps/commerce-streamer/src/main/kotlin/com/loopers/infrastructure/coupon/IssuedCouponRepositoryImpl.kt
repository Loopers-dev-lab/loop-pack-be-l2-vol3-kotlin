package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.repository.IssuedCouponRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun existsByRefCouponIdAndRefUserId(refCouponId: Long, refUserId: Long): Boolean
}

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun existsByRefCouponIdAndRefUserId(couponId: Long, userId: Long): Boolean =
        issuedCouponJpaRepository.existsByRefCouponIdAndRefUserId(couponId, userId)

    override fun save(refCouponId: Long, refUserId: Long) {
        issuedCouponJpaRepository.save(
            IssuedCouponEntity(
                refCouponId = refCouponId,
                refUserId = refUserId,
                status = IssuedCouponEntity.STATUS_AVAILABLE,
                usedAt = null,
            ),
        )
    }
}
