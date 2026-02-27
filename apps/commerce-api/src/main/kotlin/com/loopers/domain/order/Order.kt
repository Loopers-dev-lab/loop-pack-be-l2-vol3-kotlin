package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    items: List<OrderItem> = emptyList(),
) : BaseEntity() {

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    val items: MutableList<OrderItem> = items.toMutableList()

    @Column(name = "total_price", nullable = false)
    var totalPrice: Long = items.sumOf { it.productPrice * it.quantity }
        protected set
}
