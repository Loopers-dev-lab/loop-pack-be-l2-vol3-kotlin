package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "orders")
class Order protected constructor(
    val userId: Long = 0L,
) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private val _orderItems: MutableList<OrderItem> = mutableListOf()

    val orderItems: List<OrderItem>
        get() = _orderItems.toList()

    fun addOrderItem(orderItem: OrderItem) {
        _orderItems.add(orderItem)
    }

    fun getOrderDate(): ZonedDateTime = createdAt

    fun getTotalPrice(): BigDecimal {
        return _orderItems.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.getSubtotal()
        }
    }

    fun changeStatus(newStatus: OrderStatus) {
        this.status = newStatus
    }

    companion object {
        fun create(userId: Long, status: OrderStatus = OrderStatus.PENDING): Order =
            Order(userId = userId)
                .apply {
                    this.status = status
                }
    }
}
