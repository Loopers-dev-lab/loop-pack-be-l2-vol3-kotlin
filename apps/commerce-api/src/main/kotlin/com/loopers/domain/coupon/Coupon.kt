package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class Coupon(
    name: String,
    discountType: DiscountType,
    discountValue: Long,
    totalQuantity: Int,
    expiresAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var discountType: DiscountType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Long = discountValue
        protected set

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = totalQuantity
        protected set

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = 0
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: ZonedDateTime = expiresAt
        protected set

    init {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 비어있을 수 없습니다.")
        }
        if (discountValue <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인값은 양수여야 합니다.")
        }
        if (totalQuantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 발급 수량은 양수여야 합니다.")
        }
    }

    fun issue() {
        if (expiresAt.isBefore(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰입니다.")
        }
        if (issuedQuantity >= totalQuantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "발급 수량이 소진되었습니다.")
        }
        issuedQuantity++
    }
}
