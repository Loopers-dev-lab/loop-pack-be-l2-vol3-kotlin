package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.IssuedCoupon
import org.springframework.stereotype.Component

@Component
class IssuedCouponMapper {

    fun toDomain(entity: IssuedCouponEntity): IssuedCoupon {
        return IssuedCoupon(
            id = entity.id,
            couponId = entity.couponId,
            memberId = entity.memberId,
            status = CouponStatus.valueOf(entity.status),
            issuedAt = entity.issuedAt,
        )
    }

    fun toEntity(domain: IssuedCoupon): IssuedCouponEntity {
        return IssuedCouponEntity(
            couponId = domain.couponId,
            memberId = domain.memberId,
            status = domain.status.name,
            issuedAt = domain.issuedAt,
        )
    }

    fun update(entity: IssuedCouponEntity, domain: IssuedCoupon) {
        entity.status = domain.status.name
    }
}
