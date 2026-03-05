package com.loopers.application.order

import com.loopers.application.SliceResult
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderItemInfo
import com.loopers.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class GetOrderResult(
    val id: Long,
    val userId: Long,
    val username: String,
    val status: OrderStatus,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: OrderInfo, username: String): GetOrderResult {
            return GetOrderResult(
                id = info.id,
                userId = info.userId,
                username = username,
                status = info.status,
                originalPrice = info.originalPrice,
                discountAmount = info.discountAmount,
                totalPrice = info.totalPrice,
                createdAt = info.createdAt,
            )
        }
    }
}

data class GetOrderDetailResult(
    val id: Long,
    val userId: Long,
    val username: String,
    val status: OrderStatus,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val items: List<OrderItemResult>,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: OrderInfo, username: String, items: List<OrderItemResult>): GetOrderDetailResult {
            return GetOrderDetailResult(
                id = info.id,
                userId = info.userId,
                username = username,
                status = info.status,
                originalPrice = info.originalPrice,
                discountAmount = info.discountAmount,
                totalPrice = info.totalPrice,
                items = items,
                createdAt = info.createdAt,
            )
        }
    }
}

data class OrderItemResult(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun from(info: OrderItemInfo): OrderItemResult {
            return OrderItemResult(
                id = info.id,
                productId = info.productId,
                productName = info.productName,
                quantity = info.quantity,
                price = info.price,
            )
        }
    }
}

data class ListOrdersResult(
    val content: List<GetOrderResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<GetOrderResult>): ListOrdersResult {
            return ListOrdersResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
