package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "total_price", nullable = false)
    val totalPrice: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OrderStatus = OrderStatus.PENDING,
) : BaseEntity() {
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val items: List<OrderItem> = emptyList()
}

enum class OrderStatus {
    PENDING,
    PAID,
    CANCELLED,
}
