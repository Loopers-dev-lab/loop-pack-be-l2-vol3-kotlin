package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import org.springframework.stereotype.Component

@Component
class CouponMapper {

    fun toDomain(entity: CouponEntity): Coupon {
        return Coupon(
            id = entity.id,
            name = CouponName(entity.name),
            type = CouponType.valueOf(entity.type),
            discountValue = DiscountValue(entity.discountValue),
            minOrderAmount = MinOrderAmount(entity.minOrderAmount),
            expiredAt = entity.expiredAt,
        )
    }

    fun toEntity(domain: Coupon): CouponEntity {
        return CouponEntity(
            name = domain.name.value,
            type = domain.type.name,
            discountValue = domain.discountValue.value,
            minOrderAmount = domain.minOrderAmount.value,
            expiredAt = domain.expiredAt,
        )
    }

    fun update(entity: CouponEntity, domain: Coupon) {
        entity.name = domain.name.value
        entity.type = domain.type.name
        entity.discountValue = domain.discountValue.value
        entity.minOrderAmount = domain.minOrderAmount.value
        entity.expiredAt = domain.expiredAt
    }
}
