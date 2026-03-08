package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.product.Product
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
    val couponId: Long? = null,
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

    // ✅ Aggregate Root을 통해서만 OrderItem 추가 가능 (internal)
    internal fun addItem(product: Product, quantity: Int, price: BigDecimal) {
        val item = OrderItem.create(this, product, quantity, price)
        _orderItems.add(item)
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
        fun create(userId: Long, couponId: Long? = null, status: OrderStatus = OrderStatus.PENDING): Order =
            Order(userId = userId, couponId = couponId)
                .apply {
                    this.status = status
                }

        // ✅ 새 Factory 메서드
        fun createWithItems(
            userId: Long,
            couponId: Long? = null,
            items: List<OrderItemSpec>,
        ): Order {
            require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다" }

            val order = Order(userId = userId, couponId = couponId)
            items.forEach { spec ->
                order.addItem(spec.product, spec.quantity, spec.price)
            }
            return order
        }
    }
}
