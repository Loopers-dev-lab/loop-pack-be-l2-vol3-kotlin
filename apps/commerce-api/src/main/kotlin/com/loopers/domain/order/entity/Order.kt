package com.loopers.domain.order.entity

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class Order private constructor(
    refUserId: Long,
    status: OrderStatus,
    totalPrice: BigDecimal,
) : BaseEntity() {

    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long = refUserId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = status
        protected set

    @Column(name = "total_price", nullable = false)
    var totalPrice: BigDecimal = totalPrice
        protected set

    enum class OrderStatus {
        CREATED,
        PAID,
        CANCELLED,
        FAILED,
    }

    companion object {
        fun create(userId: Long, totalPrice: BigDecimal): Order {
            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                totalPrice = totalPrice,
            )
        }
    }
}
