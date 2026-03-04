package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem

class OrderInfo {

    data class Detail(
        val id: Long,
        val memberId: Long,
        val status: String,
        val totalPrice: Long,
        val orderedAt: String,
        val items: List<OrderItemInfo>,
    ) {
        companion object {
            fun from(order: Order) = Detail(
                id = requireNotNull(order.id) { "주문 저장 후 ID가 할당되지 않았습니다." },
                memberId = order.memberId,
                status = order.status.name,
                totalPrice = order.totalPrice,
                orderedAt = order.orderedAt.toString(),
                items = order.orderItems.map { OrderItemInfo.from(it) },
            )
        }
    }

    data class OrderItemInfo(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val quantity: Int,
    ) {
        companion object {
            fun from(item: OrderItem) = OrderItemInfo(
                productId = item.productId,
                productName = item.productName,
                productPrice = item.productPrice,
                quantity = item.quantity,
            )
        }
    }

    data class Main(
        val id: Long,
        val status: String,
        val totalPrice: Long,
        val orderedAt: String,
        val itemCount: Int,
    ) {
        companion object {
            fun from(order: Order) = Main(
                id = requireNotNull(order.id) { "주문 저장 후 ID가 할당되지 않았습니다." },
                status = order.status.name,
                totalPrice = order.totalPrice,
                orderedAt = order.orderedAt.toString(),
                itemCount = order.orderItems.size,
            )
        }
    }
}
