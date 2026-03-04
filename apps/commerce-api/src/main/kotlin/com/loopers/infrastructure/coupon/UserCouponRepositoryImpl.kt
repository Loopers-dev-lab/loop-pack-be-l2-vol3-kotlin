package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class UserCouponRepositoryImpl(
    private val userCouponJpaRepository: UserCouponJpaRepository,
    @PersistenceContext private val entityManager: EntityManager,
) : UserCouponRepository {

    override fun save(userCoupon: UserCoupon): UserCoupon {
        val entity = if (userCoupon.id > 0L) {
            userCouponJpaRepository.getReferenceById(userCoupon.id).apply {
                updateStatus(userCoupon.status)
                userCoupon.usedOrderId?.let { updateUsedOrderId(it) }
            }
        } else {
            UserCouponEntity.from(userCoupon)
        }
        return userCouponJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): UserCoupon? =
        userCouponJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .map { it.toDomain() }
            .orElse(null)

    override fun findByIdForUpdate(id: Long): UserCoupon? {
        val entity = userCouponJpaRepository.findById(id)
            .filter { it.deletedAt == null }
            .orElse(null) ?: return null
        // EntityManager.refresh bypasses JPA 1st-level cache and acquires FOR UPDATE lock
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE)
        return entity.toDomain()
    }

    override fun findByUserId(userId: Long): List<UserCoupon> =
        userCouponJpaRepository.findAllByUserId(userId).map { it.toDomain() }

    override fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean =
        userCouponJpaRepository.existsByUserIdAndCouponTemplateId(userId, couponTemplateId)
}
