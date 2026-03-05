package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class OrderEntity(
    userId: Long,
    totalPrice: Int,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "total_price", nullable = false)
    val totalPrice: Int = totalPrice

    fun toDomain(items: List<OrderItemEntity>): Order = Order(
        id = this.id,
        userId = this.userId,
        items = items.map { it.toDomain() },
        totalPrice = this.totalPrice,
    )

    companion object {
        fun from(order: Order): OrderEntity = OrderEntity(
            userId = order.userId,
            totalPrice = order.totalPrice,
        )
    }
}
