package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import org.springframework.stereotype.Repository

@Repository
class UserCouponRepositoryImpl(
    private val jpaRepository: UserCouponJpaRepository,
) : UserCouponRepository {

    override fun save(userCoupon: UserCoupon): Long {
        val entity = UserCouponMapper.toEntity(userCoupon)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "UserCoupon 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): UserCoupon? {
        return jpaRepository.findById(id).orElse(null)
            ?.let { UserCouponMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): UserCoupon? {
        return jpaRepository.findByIdForUpdate(id)
            ?.let { UserCouponMapper.toDomain(it) }
    }

    override fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean {
        return jpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }

    override fun findAllByUserId(userId: Long): List<UserCoupon> {
        return jpaRepository.findAllByUserId(userId)
            .map { UserCouponMapper.toDomain(it) }
    }

    override fun findAllByCouponId(couponId: Long): List<UserCoupon> {
        return jpaRepository.findAllByCouponId(couponId)
            .map { UserCouponMapper.toDomain(it) }
    }
}
