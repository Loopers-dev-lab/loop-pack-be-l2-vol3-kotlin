package com.loopers.domain.order

import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.product.Product
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "orders")
class Order protected constructor(
    val userId: Long = 0L,
    val couponId: Long? = null,
) {
    @Id
    var id: Long = 0L
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null
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

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }

    fun restore() {
        deletedAt?.let { deletedAt = null }
    }

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            id: Long,
            userId: Long,
            couponId: Long? = null,
            status: OrderStatus = OrderStatus.PENDING,
        ): Order =
            Order(userId = userId, couponId = couponId)
                .apply {
                    this.id = id
                    this.status = status
                }

        // ✅ Factory 메서드: 주문과 항목을 함께 생성
        internal fun createWithItems(
            id: Long,
            userId: Long,
            couponId: Long? = null,
            items: List<OrderItemSpec>,
        ): Order {
            require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다" }

            val order = Order(userId = userId, couponId = couponId)
                .apply { this.id = id }
            items.forEach { spec ->
                order.addItem(spec.product, spec.quantity, spec.price)
            }
            return order
        }
    }
}
