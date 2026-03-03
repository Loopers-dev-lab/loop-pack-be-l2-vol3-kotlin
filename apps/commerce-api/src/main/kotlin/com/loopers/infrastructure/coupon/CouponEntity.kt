package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class CouponEntity(
    @Column(name = "name", nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: Coupon.CouponType,
    @Column(name = "value", nullable = false)
    var value: Long,
    @Column(name = "max_discount", precision = 10, scale = 2)
    var maxDiscount: BigDecimal?,
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    var minOrderAmount: BigDecimal?,
    @Column(name = "total_quantity")
    var totalQuantity: Int?,
    @Column(name = "issued_count", nullable = false)
    var issuedCount: Int,
    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime,
) : BaseEntity() {

    companion object {
        fun fromDomain(coupon: Coupon): CouponEntity {
            return CouponEntity(
                name = coupon.name,
                type = coupon.type,
                value = coupon.value,
                maxDiscount = coupon.maxDiscount?.value,
                minOrderAmount = coupon.minOrderAmount?.value,
                totalQuantity = coupon.totalQuantity,
                issuedCount = coupon.issuedCount,
                expiredAt = coupon.expiredAt,
            ).withBaseFields(
                id = coupon.id.value,
                deletedAt = coupon.deletedAt,
            )
        }
    }

    fun toDomain(): Coupon = Coupon(
        id = CouponId(id),
        name = name,
        type = type,
        value = value,
        maxDiscount = maxDiscount?.let { Money(it) },
        minOrderAmount = minOrderAmount?.let { Money(it) },
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        expiredAt = expiredAt,
        deletedAt = deletedAt,
    )
}
