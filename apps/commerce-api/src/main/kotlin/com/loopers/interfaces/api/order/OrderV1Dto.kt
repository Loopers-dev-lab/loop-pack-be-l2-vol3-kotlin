package com.loopers.interfaces.api.order

import com.loopers.application.order.CreateOrderCriteria
import com.loopers.application.order.CreateOrderItemCriteria
import com.loopers.application.order.GetOrdersCriteria
import com.loopers.application.order.OrderItemResult
import com.loopers.application.order.OrderResult
import com.loopers.domain.Money
import java.time.ZonedDateTime

class OrderV1Dto {

    data class GetOrdersRequest(
        val startAt: String,
        val endAt: String,
    ) {
        fun toCriteria(): GetOrdersCriteria {
            return GetOrdersCriteria(
                startAt = ZonedDateTime.parse(startAt),
                endAt = ZonedDateTime.parse(endAt),
            )
        }
    }

    data class CreateOrderRequest(
        val items: List<OrderItemRequest>,
    ) {
        fun toCriteria(): CreateOrderCriteria {
            return CreateOrderCriteria(
                items = items.map {
                    CreateOrderItemCriteria(
                        productId = it.productId,
                        quantity = it.quantity,
                    )
                },
            )
        }
    }

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderResponse(
        val id: Long,
        val orderNumber: String,
        val totalAmount: Money,
        val orderStatus: String,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(result: OrderResult): OrderResponse {
                return OrderResponse(
                    id = result.id,
                    orderNumber = result.orderNumber,
                    totalAmount = result.totalAmount,
                    orderStatus = result.orderStatus,
                    items = result.items.map { OrderItemResponse.from(it) },
                    createdAt = result.createdAt,
                )
            }
        }
    }

    data class OrderItemResponse(
        val id: Long,
        val productId: Long,
        val quantity: Int,
        val itemStatus: String,
        val productName: String,
        val brandName: String,
        val imageUrl: String?,
        val originalPrice: Money,
        val discountAmount: Money,
        val finalPrice: Money,
    ) {
        companion object {
            fun from(result: OrderItemResult): OrderItemResponse {
                return OrderItemResponse(
                    id = result.id,
                    productId = result.productId,
                    quantity = result.quantity,
                    itemStatus = result.itemStatus,
                    productName = result.productName,
                    brandName = result.brandName,
                    imageUrl = result.imageUrl,
                    originalPrice = result.originalPrice,
                    discountAmount = result.discountAmount,
                    finalPrice = result.finalPrice,
                )
            }
        }
    }
}
