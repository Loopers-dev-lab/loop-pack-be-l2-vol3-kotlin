package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderDetailInfo
import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.application.order.OrderPlaceCommand
import com.loopers.domain.common.Quantity
import java.time.ZonedDateTime

class OrderDto {

    data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
        val couponId: Long? = null,
    ) {
        data class OrderItemRequest(
            val productId: Long,
            val quantity: Int,
        )

        fun toCommands(): List<OrderPlaceCommand> {
            return items.map { OrderPlaceCommand(productId = it.productId, quantity = Quantity.of(it.quantity)) }
        }
    }

    data class GetOrderResponse(
        val orderId: Long,
        val totalAmount: Long,
        val discountAmount: Long,
        val paymentAmount: Long,
        val status: String,
        val orderedAt: ZonedDateTime,
        val items: List<OrderItemResponse>,
    ) {
        data class OrderItemResponse(
            val productId: Long,
            val quantity: Int,
            val productName: String,
            val productPrice: Long,
            val brandName: String,
        ) {
            companion object {
                fun from(info: OrderItemInfo): OrderItemResponse {
                    return OrderItemResponse(
                        productId = info.productId,
                        quantity = info.quantity,
                        productName = info.productName,
                        productPrice = info.productPrice,
                        brandName = info.brandName,
                    )
                }
            }
        }

        companion object {
            fun from(info: OrderDetailInfo): GetOrderResponse {
                return GetOrderResponse(
                    orderId = info.orderId,
                    totalAmount = info.totalAmount,
                    discountAmount = info.discountAmount,
                    paymentAmount = info.paymentAmount,
                    status = info.status.name,
                    orderedAt = info.orderedAt,
                    items = info.items.map { OrderItemResponse.from(it) },
                )
            }
        }
    }

    data class GetOrdersResponse(
        val orderId: Long,
        val totalAmount: Long,
        val status: String,
        val orderedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo): GetOrdersResponse {
                return GetOrdersResponse(
                    orderId = info.orderId,
                    totalAmount = info.totalAmount,
                    status = info.status.name,
                    orderedAt = info.orderedAt,
                )
            }
        }
    }
}
