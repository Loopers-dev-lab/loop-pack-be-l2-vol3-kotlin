package com.loopers.infrastructure.coupon

import com.loopers.domain.common.Money
import com.loopers.domain.coupon.Coupon
import org.springframework.stereotype.Component

@Component
class CouponMapper {
    fun toDomain(entity: CouponEntity): Coupon {
        return Coupon.retrieve(
            id = entity.id!!,
            name = entity.name,
            type = Coupon.Type.valueOf(entity.type),
            discountValue = entity.discountValue,
            minOrderAmount = entity.minOrderAmount?.let { Money(it) },
            expiredAt = entity.expiredAt,
            deletedAt = entity.deletedAt,
        )
    }

    fun toEntity(coupon: Coupon, admin: String): CouponEntity {
        return CouponEntity(
            id = coupon.id,
            name = coupon.name,
            type = coupon.type.name,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount?.amount,
            expiredAt = coupon.expiredAt,
            createdBy = admin,
            updatedBy = admin,
        )
    }
}
