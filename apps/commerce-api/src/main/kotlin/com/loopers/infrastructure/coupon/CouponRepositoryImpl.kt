package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponMapper: CouponMapper,
) : CouponRepository {

    override fun save(coupon: Coupon): Coupon {
        val entity = resolveEntity(coupon)
        val savedEntity = couponJpaRepository.save(entity)
        return couponMapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): Coupon? {
        return couponJpaRepository.findById(id)
            .map { couponMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAllByIds(ids: List<Long>): List<Coupon> {
        return couponJpaRepository.findAllByIdIn(ids).map { couponMapper.toDomain(it) }
    }

    override fun findAll(pageable: Pageable): Page<Coupon> {
        return couponJpaRepository.findAll(pageable).map { couponMapper.toDomain(it) }
    }

    override fun deleteById(id: Long) {
        couponJpaRepository.deleteById(id)
    }

    private fun resolveEntity(coupon: Coupon): CouponEntity {
        if (coupon.id == null) return couponMapper.toEntity(coupon)

        val entity = couponJpaRepository.getReferenceById(coupon.id)
        couponMapper.update(entity, coupon)
        return entity
    }
}
