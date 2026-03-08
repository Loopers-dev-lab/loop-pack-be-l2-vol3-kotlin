package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class Coupon(
    name: String,
    discount: Discount,
    quantity: CouponQuantity,
    expiresAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Embedded
    var discount: Discount = discount
        protected set

    @Embedded
    var quantity: CouponQuantity = quantity
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: ZonedDateTime = expiresAt
        protected set

    init {
        validateName(name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 비어있을 수 없습니다.")
        }
    }

    fun update(name: String, discount: Discount, expiresAt: ZonedDateTime) {
        validateName(name)
        this.name = name
        this.discount = discount
        this.expiresAt = expiresAt
    }

    fun issue() {
        if (expiresAt.isBefore(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        quantity = quantity.issue()
    }
}
