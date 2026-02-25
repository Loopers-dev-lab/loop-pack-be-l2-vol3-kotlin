package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(
    name = "orders",
    indexes = [Index(name = "idx_orders_user_created", columnList = "user_id, created_at")],
)
class Order(
    userId: Long,
    status: OrderStatus = OrderStatus.ORDERED,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = status
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    @SQLRestriction("deleted_at IS NULL")
    private val orderItems: MutableList<OrderItem> = mutableListOf()

    val items: List<OrderItem>
        get() = orderItems.toList()

    fun addItem(
        productId: Long,
        quantity: Int,
        productName: String,
        productPrice: Long,
        brandName: String,
    ) {
        val item = OrderItem(
            order = this,
            productId = productId,
            quantity = quantity,
            productName = productName,
            productPrice = productPrice,
            brandName = brandName,
        )
        orderItems.add(item)
        calculateTotalAmount()
    }

    private fun calculateTotalAmount() {
        this.totalAmount = orderItems.sumOf { it.productPrice * it.quantity }
    }
}
