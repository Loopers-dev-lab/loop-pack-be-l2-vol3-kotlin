package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class OrderModel(
    userId: Long,
    status: OrderStatus = OrderStatus.ORDERED,
    originalPrice: BigDecimal,
    discountAmount: BigDecimal = BigDecimal.ZERO,
    totalPrice: BigDecimal,
    issuedCouponId: Long? = null,
) : BaseEntity() {
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = status
        protected set

    @Column(name = "original_price", nullable = false, precision = 12, scale = 2)
    var originalPrice: BigDecimal = originalPrice
        protected set

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    var discountAmount: BigDecimal = discountAmount
        protected set

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = totalPrice
        protected set

    @Column(name = "issued_coupon_id")
    var issuedCouponId: Long? = issuedCouponId
        protected set

    init {
        validateOriginalPrice(originalPrice)
        validateDiscountAmount(discountAmount)
        validateTotalPrice(totalPrice)
    }

    private fun validateOriginalPrice(originalPrice: BigDecimal) {
        if (originalPrice <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "원래 주문 금액은 0보다 커야 합니다.")
        }
    }

    private fun validateDiscountAmount(discountAmount: BigDecimal) {
        if (discountAmount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.")
        }
    }

    private fun validateTotalPrice(totalPrice: BigDecimal) {
        if (totalPrice <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 주문 금액은 0보다 커야 합니다.")
        }
    }

    fun cancel() {
        if (status != OrderStatus.ORDERED) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 완료 상태에서만 취소할 수 있습니다.")
        }
        status = OrderStatus.CANCELLED
    }
}
