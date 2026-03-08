package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponName
import com.loopers.domain.product.Money

object CouponMapper {

    fun toDomain(entity: CouponEntity): Coupon {
        val id = requireNotNull(entity.id) {
            "CouponEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Coupon.reconstitute(
            persistenceId = id,
            name = CouponName(entity.name),
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            minOrderAmount = Money(entity.minOrderAmount),
            maxIssueCount = entity.maxIssueCount,
            issuedCount = entity.issuedCount,
            expiredAt = entity.expiredAt,
            deletedAt = entity.deletedAt,
        )
    }

    fun toEntity(domain: Coupon): CouponEntity {
        return CouponEntity(
            id = domain.persistenceId,
            name = domain.name.value,
            discountType = domain.discountType,
            discountValue = domain.discountValue,
            minOrderAmount = domain.minOrderAmount.amount,
            maxIssueCount = domain.maxIssueCount,
            issuedCount = domain.issuedCount,
            expiredAt = domain.expiredAt,
            deletedAt = domain.deletedAt,
        )
    }
}
