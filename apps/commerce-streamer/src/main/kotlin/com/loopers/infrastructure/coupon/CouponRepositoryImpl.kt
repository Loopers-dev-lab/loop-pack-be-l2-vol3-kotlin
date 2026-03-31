package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponMapper: CouponMapper,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon {
        val admin = "SYSTEM"
        val entity = if (coupon.id != null) {
            val existing = couponJpaRepository.findById(coupon.id).orElseThrow()
            existing.name = coupon.name
            existing.discountValue = coupon.discountValue
            existing.minOrderAmount = coupon.minOrderAmount
            existing.expiredAt = coupon.expiredAt
            existing.issueLimit = coupon.issueLimit
            existing.issuedCount = coupon.issuedCount
            if (coupon.isDeleted()) existing.deleteBy(admin) else existing.updateBy(admin)
            existing
        } else {
            couponMapper.toEntity(coupon, admin)
        }
        return couponMapper.toDomain(couponJpaRepository.saveAndFlush(entity))
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)
            ?.let { couponMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): Coupon? {
        return couponJpaRepository.findByIdForUpdate(id)
            ?.let { couponMapper.toDomain(it) }
    }
}
