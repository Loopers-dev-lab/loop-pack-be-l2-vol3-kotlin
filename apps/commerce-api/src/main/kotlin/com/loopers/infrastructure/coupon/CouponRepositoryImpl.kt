package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import org.springframework.stereotype.Repository

@Repository
class CouponRepositoryImpl(
    private val jpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: Coupon): Long {
        val entity = CouponMapper.toEntity(coupon)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "Coupon 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): Coupon? {
        return jpaRepository.findById(id).orElse(null)
            ?.let { CouponMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): Coupon? {
        return jpaRepository.findByIdForUpdate(id)
            ?.let { CouponMapper.toDomain(it) }
    }

    override fun incrementIssuedCount(id: Long): Int {
        return jpaRepository.incrementIssuedCount(id)
    }

    override fun findAll(): List<Coupon> {
        return jpaRepository.findAll().map { CouponMapper.toDomain(it) }
    }
}
