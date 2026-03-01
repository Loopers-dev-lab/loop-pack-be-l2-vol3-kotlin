package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class Discount(
    type: DiscountType,
    value: Long,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var type: DiscountType = type
        protected set

    @Column(name = "discount_value", nullable = false)
    var value: Long = value
        protected set

    init {
        if (value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인값은 양수여야 합니다.")
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Discount && type == other.type && value == other.value)

    override fun hashCode(): Int = 31 * type.hashCode() + value.hashCode()

    override fun toString(): String = "$type($value)"
}
