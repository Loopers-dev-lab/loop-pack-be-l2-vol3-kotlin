package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class CouponModel(
    name: String,
    discountType: DiscountType,
    discountValue: Int,
    totalQuantity: Int,
    issuedQuantity: Int = 0,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var discountType: DiscountType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Int = discountValue
        protected set

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = totalQuantity
        protected set

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = issuedQuantity
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    fun issue() {
        if (isExpired()) {
            throw IllegalStateException("만료된 쿠폰입니다.")
        }
        if (issuedQuantity >= totalQuantity) {
            throw IllegalStateException("쿠폰 발급 수량이 초과되었습니다.")
        }
        issuedQuantity++
    }

    fun isExpired(): Boolean {
        return expiredAt.isBefore(ZonedDateTime.now())
    }
}
