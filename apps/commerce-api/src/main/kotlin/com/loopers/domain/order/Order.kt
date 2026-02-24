package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    userId: Long,
    totalAmount: Long,
    status: OrderStatus = OrderStatus.ORDERED,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = totalAmount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = status
        protected set

    init {
        validateTotalAmount(totalAmount)
    }

    private fun validateTotalAmount(totalAmount: Long) {
        if (totalAmount < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "총 금액은 0 이상이어야 합니다.")
        }
    }
}
