package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.product.Money

object UserCouponMapper {

    fun toDomain(entity: UserCouponEntity): UserCoupon {
        val id = requireNotNull(entity.id) {
            "UserCouponEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return UserCoupon.reconstitute(
            persistenceId = id,
            refCouponId = entity.couponId,
            refUserId = entity.userId,
            status = entity.status,
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            minOrderAmount = Money(entity.minOrderAmount),
            expiredAt = entity.expiredAt,
            usedAt = entity.usedAt,
            issuedAt = entity.issuedAt,
        )
    }

    fun toEntity(domain: UserCoupon): UserCouponEntity {
        return UserCouponEntity(
            id = domain.persistenceId,
            couponId = domain.refCouponId,
            userId = domain.refUserId,
            status = domain.status,
            discountType = domain.discountType,
            discountValue = domain.discountValue,
            minOrderAmount = domain.minOrderAmount.amount,
            expiredAt = domain.expiredAt,
            usedAt = domain.usedAt,
            issuedAt = domain.issuedAt,
        )
    }
}
