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
    totalPrice: BigDecimal,
) : BaseEntity() {
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = status
        protected set

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = totalPrice
        protected set

    init {
        validateTotalPrice(totalPrice)
    }

    private fun validateTotalPrice(totalPrice: BigDecimal) {
        if (totalPrice <= BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 주문 금액은 0보다 커야 합니다.")
        }
    }
}
