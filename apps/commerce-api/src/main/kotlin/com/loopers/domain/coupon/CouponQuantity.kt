package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class CouponQuantity(
    total: Int,
    issued: Int,
) {
    @Column(name = "total_quantity", nullable = false)
    var total: Int = total
        protected set

    @Column(name = "issued_quantity", nullable = false)
    var issued: Int = issued
        protected set

    init {
        if (total <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 발급 수량은 양수여야 합니다.")
        }
        if (issued < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량은 0 이상이어야 합니다.")
        }
    }

    fun issue(): CouponQuantity {
        if (isExhausted()) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량이 소진되었습니다.")
        }
        return CouponQuantity(total, issued + 1)
    }

    fun isExhausted(): Boolean = issued >= total

    override fun equals(other: Any?): Boolean =
        this === other || (other is CouponQuantity && total == other.total && issued == other.issued)

    override fun hashCode(): Int = 31 * total + issued

    override fun toString(): String = "$issued/$total"
}
