package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponMapper: CouponMapper,
) : CouponRepository {
    override fun save(coupon: Coupon): Coupon {
        val admin = "ADMIN"
        return couponMapper.toDomain(
            couponJpaRepository.saveAndFlush(couponMapper.toEntity(coupon, admin)),
        )
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findByIdAndDeletedAtIsNull(id)?.let { couponMapper.toDomain(it) }
    }

    override fun findAll(pageable: Pageable): Page<Coupon> {
        return couponJpaRepository.findAllByDeletedAtIsNull(pageable).map { couponMapper.toDomain(it) }
    }
}
