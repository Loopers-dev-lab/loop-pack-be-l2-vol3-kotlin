package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderUseCase
import com.loopers.application.order.OrderInfo
import jakarta.validation.constraints.NotEmpty

class OrderV1Dto {

    data class CreateRequest(@field:NotEmpty val items: List<OrderItemRequest>) {
        fun toCommand() = OrderUseCase.CreateOrderCommand(
            items = items.map {
                OrderUseCase.OrderItemRequest(productId = it.productId, quantity = it.quantity)
            },
        )
    }

    data class OrderItemRequest(val productId: Long, val quantity: Int)

    data class DetailResponse(
        val id: Long,
        val memberId: Long,
        val status: String,
        val totalPrice: Long,
        val orderedAt: String,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(info: OrderInfo.Detail) = DetailResponse(
                id = info.id,
                memberId = info.memberId,
                status = info.status,
                totalPrice = info.totalPrice,
                orderedAt = info.orderedAt,
                items = info.items.map { OrderItemResponse.from(it) },
            )
        }
    }

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val productPrice: Long,
        val quantity: Int,
    ) {
        companion object {
            fun from(info: OrderInfo.OrderItemInfo) = OrderItemResponse(
                productId = info.productId,
                productName = info.productName,
                productPrice = info.productPrice,
                quantity = info.quantity,
            )
        }
    }

    data class MainResponse(
        val id: Long,
        val status: String,
        val totalPrice: Long,
        val orderedAt: String,
        val itemCount: Int,
    ) {
        companion object {
            fun from(info: OrderInfo.Main) = MainResponse(
                id = info.id,
                status = info.status,
                totalPrice = info.totalPrice,
                orderedAt = info.orderedAt,
                itemCount = info.itemCount,
            )
        }
    }
}
