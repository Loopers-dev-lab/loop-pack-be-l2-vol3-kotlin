package com.loopers.application.order

import com.loopers.application.SliceResult
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderItemInfo
import com.loopers.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateOrderResult(
    val id: Long,
) {
    companion object {
        fun from(info: OrderInfo): CreateOrderResult {
            return CreateOrderResult(id = info.id)
        }
    }
}

data class UserGetOrderResult(
    val id: Long,
    val status: OrderStatus,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val items: List<UserGetOrderItemResult>,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(info: OrderInfo): UserGetOrderResult {
            return UserGetOrderResult(
                id = info.id,
                status = info.status,
                originalPrice = info.originalPrice,
                discountAmount = info.discountAmount,
                totalPrice = info.totalPrice,
                items = info.items.map { UserGetOrderItemResult.from(it) },
                createdAt = info.createdAt,
            )
        }
    }
}

data class UserGetOrderItemResult(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun from(info: OrderItemInfo): UserGetOrderItemResult {
            return UserGetOrderItemResult(
                id = info.id,
                productId = info.productId,
                productName = info.productName,
                quantity = info.quantity,
                price = info.price,
            )
        }
    }
}

data class CancelOrderResult(
    val id: Long,
) {
    companion object {
        fun from(info: OrderInfo): CancelOrderResult = CancelOrderResult(id = info.id)
    }
}

data class UserGetOrdersResult(
    val content: List<UserGetOrderResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<UserGetOrderResult>): UserGetOrdersResult {
            return UserGetOrdersResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
