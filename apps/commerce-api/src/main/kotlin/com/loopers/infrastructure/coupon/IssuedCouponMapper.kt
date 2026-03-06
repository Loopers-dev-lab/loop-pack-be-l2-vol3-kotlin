package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.stereotype.Component

@Component
class IssuedCouponMapper {
    fun toDomain(entity: IssuedCouponEntity): IssuedCoupon {
        return IssuedCoupon.retrieve(
            id = entity.id!!,
            couponId = entity.couponId,
            userId = entity.userId,
            status = IssuedCoupon.Status.valueOf(entity.status),
            expiredAt = entity.expiredAt,
            usedAt = entity.usedAt,
            version = entity.version,
        )
    }

    fun toEntity(issuedCoupon: IssuedCoupon): IssuedCouponEntity {
        return IssuedCouponEntity(
            id = issuedCoupon.id,
            couponId = issuedCoupon.couponId,
            userId = issuedCoupon.userId,
            status = issuedCoupon.status.name,
            expiredAt = issuedCoupon.expiredAt,
            usedAt = issuedCoupon.usedAt,
            version = issuedCoupon.version,
        )
    }
}
