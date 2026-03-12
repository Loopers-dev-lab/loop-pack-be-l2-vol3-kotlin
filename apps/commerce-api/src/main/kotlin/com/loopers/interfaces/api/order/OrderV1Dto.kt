package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderItemResult
import com.loopers.application.order.OrderResult
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem

class OrderV1Dto {

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
        val userCouponId: Long? = null,
    )

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val brandId: Long,
        val brandName: String,
        val price: Int,
        val quantity: Int,
        val subtotal: Int,
    ) {
        companion object {
            fun from(result: OrderItemResult) = OrderItemResponse(
                productId = result.productId,
                productName = result.productName,
                brandId = result.brandId,
                brandName = result.brandName,
                price = result.price,
                quantity = result.quantity,
                subtotal = result.subtotal,
            )

            fun from(item: OrderItem) = OrderItemResponse(
                productId = item.productId,
                productName = item.productName,
                brandId = item.brandId,
                brandName = item.brandName,
                price = item.price,
                quantity = item.quantity,
                subtotal = item.subtotal(),
            )
        }
    }

    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val originalTotalPrice: Int,
        val discountAmount: Int,
        val totalPrice: Int,
        val userCouponId: Long?,
        val items: List<OrderItemResponse>,
    ) {
        companion object {
            fun from(result: OrderResult) = OrderResponse(
                id = result.id,
                userId = result.userId,
                originalTotalPrice = result.originalTotalPrice,
                discountAmount = result.discountAmount,
                totalPrice = result.totalPrice,
                userCouponId = result.userCouponId,
                items = result.items.map { OrderItemResponse.from(it) },
            )

            fun from(order: Order) = OrderResponse(
                id = order.id,
                userId = order.userId,
                originalTotalPrice = order.originalTotalPrice,
                discountAmount = order.discountAmount,
                totalPrice = order.totalPrice,
                userCouponId = order.userCouponId,
                items = order.items.map { OrderItemResponse.from(it) },
            )
        }
    }
}
