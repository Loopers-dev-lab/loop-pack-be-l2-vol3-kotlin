package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val issuedCouponMapper: IssuedCouponMapper,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        val entity = resolveEntity(issuedCoupon)
        try {
            val savedEntity = issuedCouponJpaRepository.save(entity)
            return issuedCouponMapper.toDomain(savedEntity)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.DUPLICATE_COUPON_ISSUE)
        }
    }

    override fun findById(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findById(id)
            .map { issuedCouponMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByIdForUpdate(id: Long): IssuedCoupon? {
        return issuedCouponJpaRepository.findByIdForUpdate(id)
            ?.let { issuedCouponMapper.toDomain(it) }
    }

    override fun findAllByMemberId(memberId: Long): List<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByMemberId(memberId)
            .map { issuedCouponMapper.toDomain(it) }
    }

    override fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean {
        return issuedCouponJpaRepository.existsByCouponIdAndMemberId(couponId, memberId)
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCoupon> {
        return issuedCouponJpaRepository.findAllByCouponId(couponId, pageable)
            .map { issuedCouponMapper.toDomain(it) }
    }

    private fun resolveEntity(issuedCoupon: IssuedCoupon): IssuedCouponEntity {
        if (issuedCoupon.id == null) return issuedCouponMapper.toEntity(issuedCoupon)

        val entity = issuedCouponJpaRepository.getReferenceById(issuedCoupon.id)
        issuedCouponMapper.update(entity, issuedCoupon)
        return entity
    }
}
