package com.loopers.interfaces.api.order

import com.loopers.domain.order.dto.OrderedInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

class OrderV1Dto {

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderRequest(
        @field:NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        @field:Valid
        val orderItems: List<OrderItemRequest>,
    )

    data class OrderResponse(
        val orderId: Long,
        val orderDate: String,
        val totalPrice: String,
        val orderItems: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(orderedInfo: OrderedInfo): OrderResponse = OrderResponse(
                orderId = orderedInfo.orderId,
                orderDate = orderedInfo.orderDate.toString(),
                totalPrice = orderedInfo.totalPrice.toString(),
                orderItems = orderedInfo.orderItems.map { OrderItemResponse.from(it) },
            )
        }

        data class OrderItemResponse(
            val productId: Long,
            val productName: String,
            val price: String,
            val quantity: Int,
            val subtotal: String,
        ) {
            companion object {
                fun from(itemInfo: OrderedInfo.OrderedItemInfo): OrderItemResponse = OrderItemResponse(
                    productId = itemInfo.productId,
                    productName = itemInfo.productName,
                    price = itemInfo.price.toString(),
                    quantity = itemInfo.quantity,
                    subtotal = itemInfo.subtotal.toString(),
                )
            }
        }
    }
}
