package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class Order private constructor(
    refUserId: Long,
    status: OrderStatus,
    totalPrice: BigDecimal,
    items: MutableList<OrderItem>,
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

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "ref_order_id", nullable = false)
    var items: MutableList<OrderItem> = items
        protected set

    enum class OrderStatus {
        CREATED,
        PAID,
        CANCELLED,
        FAILED,
    }

    companion object {
        fun create(
            userId: Long,
            products: List<OrderProductInfo>,
            command: OrderCommand.CreateOrder,
        ): Order {
            if (command.items.isEmpty()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
            }

            val productIds = command.items.map { it.productId }
            if (productIds.size != productIds.toSet().size) {
                throw CoreException(ErrorType.BAD_REQUEST, "중복된 상품이 포함되어 있습니다.")
            }

            val productMap = products.associateBy { it.id }

            val orderItems = command.items.map { item ->
                val product = productMap[item.productId]
                    ?: throw CoreException(ErrorType.BAD_REQUEST, "상품을 찾을 수 없습니다.")
                OrderItem.create(product, item.quantity)
            }

            val totalPrice = orderItems.fold(BigDecimal.ZERO) { acc, orderItem ->
                acc + orderItem.productPrice.multiply(BigDecimal(orderItem.quantity))
            }

            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                totalPrice = totalPrice,
                items = orderItems.toMutableList(),
            )
        }
    }
}
