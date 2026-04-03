package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponInfo
import com.loopers.domain.coupon.CouponIssueRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class CouponIssueRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val userCouponJpaRepository: UserCouponJpaRepository,
) : CouponIssueRepository {

    override fun findCouponById(id: Long): CouponInfo? {
        return couponJpaRepository.findById(id).orElse(null)?.let { entity ->
            CouponInfo(
                id = requireNotNull(entity.id),
                discountType = entity.discountType,
                discountValue = entity.discountValue,
                minOrderAmount = entity.minOrderAmount,
                expiredAt = entity.expiredAt,
                isDeleted = entity.deletedAt != null,
            )
        }
    }

    override fun incrementIssuedCount(couponId: Long): Int {
        return couponJpaRepository.incrementIssuedCount(couponId)
    }

    override fun existsUserCoupon(couponId: Long, userId: Long): Boolean {
        return userCouponJpaRepository.existsByCouponIdAndUserId(couponId, userId)
    }

    override fun saveUserCoupon(
        couponId: Long,
        userId: Long,
        discountType: String,
        discountValue: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    ) {
        val entity = UserCouponEntity(
            id = null,
            couponId = couponId,
            userId = userId,
            status = "AVAILABLE",
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
            usedAt = null,
            issuedAt = ZonedDateTime.now(),
        )
        userCouponJpaRepository.save(entity)
    }
}
